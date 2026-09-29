# 制造业质量追溯 API 服务（quality-trace）

把散在纸上的质量记录串成一条链：**工单开工 → 批次投料 → 检验录入 → 不良处置**，按批次号可查到完整链路。纯后端服务（Spring Boot 3 + Java 17 + MyBatis-Plus + PostgreSQL 15）。

## 快速启动

```bash
cp .env.example .env && docker compose up -d
```

启动后：

- 健康检查：<http://localhost:21114/health>
- 接口前缀：`http://localhost:21114/api`
- 数据库首次启动自动执行 `database/init.sql`（含表结构与种子数据）

## 核心业务保证（本项目如何实现需求）

1. **按批次号查完整链路**
   `GET /api/trace/{batchNo}` 返回追溯树：工单（开工事件）→ 批次（投料事件）→ 历次检验（含每个检验项）→ 不良记录与每次处置。

2. **同批次多条检验同时回传，后到的接住前一批的状态和数量，不能覆盖**
   - 检验提交在一个事务内先执行 `SELECT … FOR UPDATE` 锁住批次行，并发回传在此排队串行；
   - 拿锁后**重读**批次并重放 `state_event` 得到前态，合格/不良数量在现值上**累加接续**（`qualifiedQtyBefore/After` 写进事件摘要）；
   - 支持请求头 `Idempotency-Key`：同批次同键的重复回传直接回放首次结果，不重复计数（数据库唯一索引兜底）。

3. **检验写到一半失败，整批不留半条记录**
   检验主表 + 检验项 + 数量回写 + 状态事件在同一个数据库事务，任一环节抛异常全部回滚。测试用例 `failedInspectionLeavesNoPartialRecords` 断言失败后主表、检验项、检验事件数量均为 0。

4. **状态变化每次留记录，当前状态由记录推出，不直接覆盖旧值**
   工单表、批次表、不良表**都没有可更新的状态列**；所有状态变化只向 `state_event` 追加一条事件（含 `from_state/to_state/change_summary`），当前状态取该聚合最新事件的 `to_state`。

5. **无批次号的旧工单仍可查，不强制回填**
   `product_batch.work_order_id` 可空；种子工单 `WO-2025-LEGACY-01` 没有任何批次，列表/详情照常返回（`hasBatch=false`）。批次也允许不挂工单创建。

6. **关闭不良处置后，原判定和变更前后值仍保留**
   `defect_record.original_result / original_severity` 登记后不可变；处置流转 `OPEN → IN_DISPOSITION → REWORKING/CONCESSION/SCRAPPED → CLOSED` 全部事件化，关闭事件同时记录原判定与前后值。关闭后再发起流转返回 409。

## CLI 示例

```bash
# 1) 登录拿 token（种子账号见下）
TOKEN=$(curl -s localhost:21114/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"supervisor","password":"super123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 2) 创建工单并开工
curl -s -XPOST localhost:21114/api/work-orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"WO-2026-1001","productCode":"P-GEAR-07","productName":"7号齿轮","plannedQty":1000,"lineCode":"L-A1"}'
curl -s -XPOST localhost:21114/api/work-orders/<工单id>/start -H "Authorization: Bearer $TOKEN"

# 3) 批次投料
curl -s -XPOST localhost:21114/api/batches -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"batchNo":"B2026100101","workOrderId":<工单id>,"quantity":200,"materialLotNo":"MAT-X"}'

# 4) 检验录入（质检员；支持 Idempotency-Key）
ITOKEN=$(curl -s localhost:21114/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"inspector","password":"inspector123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -s -XPOST localhost:21114/api/inspections -H "Authorization: Bearer $ITOKEN" \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: insp-2026100101-01' \
  -d '{"batchNo":"B2026100101","inspectionType":"FIRST","standardVersion":"STD-V1",
       "qualifiedQty":198,"defectQty":2,
       "items":[{"itemCode":"DIM-01","itemName":"齿顶圆","measuredValue":49.98,"limitMin":49.90,"limitMax":50.10}]}'

# 5) 登记不良 + 处置 + 关闭
curl -s -XPOST localhost:21114/api/defects -H "Authorization: Bearer $ITOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"batchNo":"B2026100101","defectType":"磕碰","defectQty":2,"severity":"MINOR","rootCause":"工装老化","originalResult":"REWORK"}'
curl -s -XPOST localhost:21114/api/defects/<不良id>/disposition -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"targetStatus":"IN_DISPOSITION"}'
curl -s -XPOST localhost:21114/api/defects/<不良id>/close -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"dispositionNote":"返工复检合格，关闭"}'

# 6) 按批次号查完整链路
curl -s localhost:21114/api/trace/B2026100101 -H "Authorization: Bearer $TOKEN"
```

### 种子账号（角色 RBAC）

| 用户名 | 密码 | 角色 | 权限 |
|---|---|---|---|
| inspector | inspector123 | INSPECTOR 质检员 | 检验录入、不良登记 |
| supervisor | super123 | SUPERVISOR 产线主管 | 工单、批次、不良处置 |
| manager | manager123 | MANAGER 质量经理 | 全部写操作 |
| auditor | auditor123 | AUDITOR 审计员 | 只读 |

## API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/login | 登录获取 JWT |
| POST | /api/work-orders | 创建工单 |
| POST | /api/work-orders/{id}/start | 开工（PLANNED/PAUSED→RUNNING） |
| POST | /api/work-orders/{id}/pause | 暂停 |
| POST | /api/work-orders/{id}/finish | 完工 |
| POST | /api/work-orders/{id}/cancel | 取消 |
| GET  | /api/work-orders, /api/work-orders/{id} | 工单列表/详情（含事件流） |
| POST | /api/batches | 批次投料（workOrderId 可空） |
| POST | /api/inspections | 检验整批录入（头 Idempotency-Key 可选） |
| GET  | /api/inspection-items/by-inspection/{id} | 检验项查询 |
| POST | /api/defects | 登记不良（原判定不可改） |
| POST | /api/defects/{id}/disposition | 处置流转 |
| POST | /api/defects/{id}/close | 关闭处置（须填关闭结论） |
| GET  | /api/trace/{batchNo} | 批次全链路追溯树 |

## 本地开发方式

前置：JDK 17、Maven 3.9+、PostgreSQL 15。

```bash
# 启动一个本地 PG 后执行 database/init.sql，再运行：
cd backend
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--DB_HOST=localhost --DB_PORT=54320 --DB_NAME=app_db --DB_USER=app_user --DB_PASSWORD=app_password"
```

运行测试（使用内嵌 PostgreSQL，覆盖并发接续/失败回滚/关闭留痕/RBAC）：

```bash
cd backend && mvn test
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3 + Java 17 + MyBatis-Plus 3.5 + Spring JDBC（行锁） |
| 认证 | JWT（jjwt 0.12）+ RBAC 四角色 |
| 数据库 | PostgreSQL 15（jsonb、SELECT … FOR UPDATE、部分唯一索引） |
| 测试 | JUnit 5 + embedded-postgres |
| 部署 | Docker Compose（命名卷 + healthcheck + depends_on: service_healthy） |

## 项目目录结构

```text
backend/src/main/java/com/generated/qualityTrace/
├── routes/               # 按实体登记路由与 RBAC 矩阵（RouteRegistry 汇总）
├── controllers/          # 按实体分文件，仅做参数装配
├── services/             # 事务/行锁/事件溯源/状态机核心
├── models/               # MyBatis-Plus 实体（无可变状态列）
├── repositories/         # Mapper + 悲观行锁 + jsonb 类型处理器
├── middlewares/          # Auth/Rbac/RateLimit/AuditLog 过滤器 + 全局异常 + JWT
├── constants/            # 枚举、错误码、错误消息、日志模板、事件类型、角色
├── constructors/         # 响应 DTO 工厂
├── validators/           # 领域规则校验
├── exceptions/           # 业务异常分层
├── types/                # 请求 Payload（Bean Validation）
├── utils/                # Formatters（日期/数量/状态文案）
└── config/               # ObjectMapper 等全局配置
database/init.sql         # 表结构 + 种子数据（测试复用同一脚本）
```

## 数据模型要点

- `state_event`：事件流，`(aggregate_type, aggregate_id, id)` 重放；`(aggregate_type, aggregate_id, idempotency_key)` 部分唯一索引支撑幂等回放；`change_summary` 为 jsonb。
- `work_order` / `product_batch` / `defect_record`：**无状态列**，状态全部由事件推出。
- 数量字段（completed_qty/qualified_qty/defect_qty）只在事务内、行锁之后累加。

## 环境变量说明

- `COMPOSE_PROJECT_NAME`：Compose 项目名，默认 `quality-trace`
- `BACKEND_PORT`：后端宿主机端口，默认 `21114`
- `DB_PORT`：数据库宿主机端口，默认 `54320`
- `DB_NAME / DB_USER / DB_PASSWORD`：数据库名与凭据
- `JWT_SECRET / JWT_EXPIRE_SECONDS`：JWT 签名密钥与有效期（秒）
- `RATE_LIMIT_PER_MINUTE`：每主体每分钟请求限额，默认 300

新增配置需同步：`.env.example`、`.env`、`docker-compose.yml`、`backend/src/main/resources/application.yml`。

## Docker 部署说明

- 根 Compose 不写 `version:`，顶层 `name: quality-trace`；容器名均带 `${COMPOSE_PROJECT_NAME:-quality-trace}` 前缀。
- 端口映射 `${BACKEND_PORT:-21114}:8080`；数据库用命名卷 `db_data`，不绑定中文路径。
- DB 配置 healthcheck，后端 `depends_on: condition: service_healthy`；后端自身用 wget 探测 `/health`。
- 常见问题：
  - 端口占用：改 `.env` 中端口后 `docker compose up -d`。
  - 重置数据并重新初始化：`docker compose down -v && docker compose up -d`。
  - 在任意目录名（含中文）下均可启动，路径只用于构建上下文，数据不落到宿主目录。

## 枚举/常量出现位置清单

- **WorkOrderStatus（PLANNED/RUNNING/PAUSED/FINISHED/CANCELLED）**
  `constants/WorkOrderStatus`（枚举与允许流转集合）、`services/WorkOrderService`（状态机）、`controllers/WorkOrderController`、`constructors/WorkOrderDtoFactory`（经 `utils/Formatters.statusText` 出中文文案）、`constants/LogTemplates`（WO_*）、`constants/ErrorMessages`（WORK_ORDER_CONFLICT）、`database/init.sql`（种子事件）、README。
- **InspectionResultStatus（PASS/FAIL/CONDITIONAL_PASS/RECHECK）**
  `constants/InspectionResultStatus`、`validators/DomainValidator`（判定）、`services/QualityInspectionService`（结论→事件→批次状态）、`constructors/QualityInspectionDtoFactory`、`constants/LogTemplates`（INS_*/BATCH_INSPECT）、`constants/ErrorMessages`（INSPECTION_*）、`database/init.sql`（check 约束与种子）、README。
- **DefectSeverity（MINOR/MAJOR/CRITICAL）**
  `constants/DefectSeverity`（canConcession）、`validators/DomainValidator`、`services/DefectRecordService`（让步限制）、`constructors/DefectRecordDtoFactory`、`constants/LogTemplates`（DEF_*）、`constants/ErrorMessages`（DEFECT_*）、`types/DefectRecordPayload`、`database/init.sql`（check 约束）、README。

## 横切关注点

- **JWT + RBAC**：`AuthMiddleware` 校验令牌写入 `CurrentUser`，`RbacMiddleware` 读取 `routes/*` 登记的角色矩阵。
- **审计 + 追溯事件**：`AuditLogMiddleware` 把所有写请求落 `audit_log`；业务状态变化落 `state_event`，二者互补。
- **全局异常 + 限流**：`ErrorHandlerMiddleware` 统一错误出参（service/controller 各自包装业务异常，不在全局吞错误码）；`RateLimitMiddleware` 按用户/IP 滑动窗口限流（429）。

## 为什么会牵一发动全身

日志模板（`LogTemplates`）、错误码（`ErrorCodes`）、错误消息（`ErrorMessages`）、DTO 工厂（`constructors/`）、枚举（`constants/`）、格式化（`Formatters`）分别独立成文件，被 controller/service/中间件多层直接引用。新增一个状态值通常要同步：枚举、状态机、事件类型、日志模板、错误消息、DTO 文案、数据库 check 约束与种子、README。

## License

MIT
