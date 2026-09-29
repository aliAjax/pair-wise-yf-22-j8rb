# 制造业质量追溯 API 服务

面向小型制造工厂的批次质量追溯后端服务，把**工单开工、批次投料、检验录入、不良处置**串成可按批次号查询的完整链路。系统采用事件追加方式保存状态变化：当前状态由最新状态记录推导，不直接覆盖历史状态。

## 快速启动

```bash
cp .env.example .env && docker compose up -d
```

健康检查：

```bash
curl http://localhost:21114/health
```

## 端到端 CLI 示例

以下账号已由 `database/init.sql` 初始化。

```bash
# 1. 产线主管登录
TOKEN=$(curl -s http://localhost:21114/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"supervisor","password":"supervisor123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# 2. 创建工单
curl -s -X POST http://localhost:21114/api/work-orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"orderNo":"WO-20260929-001","productCode":"P-MOTOR-01","productName":"驱动电机","plannedQty":100,"lineCode":"LINE-A"}'

# 3. 工单开工
curl -s -X POST http://localhost:21114/api/work-orders/2/start \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"note":"首班开工"}'

# 4. 批次投料
curl -s -X POST http://localhost:21114/api/batches \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"batchNo":"B-20260929-001","workOrderId":2,"quantity":50,"materialLotNo":"MAT-001"}'
```

质检员录入检验：

```bash
ITOKEN=$(curl -s http://localhost:21114/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"inspector","password":"inspector123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -s -X POST http://localhost:21114/api/inspections \
  -H "Authorization: Bearer $ITOKEN" -H 'Content-Type: application/json' \
  -d '{
    "batchNo":"B-20260929-001",
    "inspectionType":"FIRST",
    "standardVersion":"STD-V3",
    "items":[
      {"itemCode":"DIM-X","itemName":"X向尺寸","measuredValue":10.02,"limitMin":9.90,"limitMax":10.10},
      {"itemCode":"RES-Y","itemName":"Y向电阻","measuredValue":21.8,"limitMin":20.0,"limitMax":22.0}
    ]
  }'

curl -s http://localhost:21114/api/trace/B-20260929-001 \
  -H "Authorization: Bearer $ITOKEN"
```

质量经理登记/处置/关闭不良：

```bash
MTOKEN=$(curl -s http://localhost:21114/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"manager123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -s -X POST http://localhost:21114/api/defects \
  -H "Authorization: Bearer $MTOKEN" -H 'Content-Type: application/json' \
  -d '{"batchNo":"B-20260929-001","defectType":"划伤","defectQty":2,"severity":"MAJOR","rootCause":"转运工装毛刺"}'

curl -s -X POST http://localhost:21114/api/defects/1/disposition \
  -H "Authorization: Bearer $MTOKEN" -H 'Content-Type: application/json' \
  -d '{"dispositionStatus":"REWORK","note":"返工抛光后复检"}'

curl -s -X POST http://localhost:21114/api/defects/1/close \
  -H "Authorization: Bearer $MTOKEN" -H 'Content-Type: application/json' \
  -d '{"note":"复检合格，关闭"}'
```

旧工单没有批次号也不强制回填，仍可通过以下接口查询：

```bash
curl -s http://localhost:21114/api/work-orders/order/WO-LEGACY-001 \
  -H "Authorization: Bearer $TOKEN"
```

## 本地开发方式

- 后端：进入 `backend` 后执行 `mvn spring-boot:run`。
- 数据库：PostgreSQL 15；本地可用根目录 Compose 只启动 `db`：`docker compose up -d db`。
- 接口统一挂在 `/api`，健康检查为 `/health`。
- 表结构同时位于 `database/init.sql` 和 `backend/src/main/resources/schema.sql`，应用启动时会幂等初始化。

## 访问地址

- API Base：`http://localhost:21114/api`
- 健康检查：`http://localhost:21114/health`
- 批次追溯：`GET /api/trace/{batchNo}`

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3 + Java 17 + Spring MVC |
| 持久层 | Spring JdbcTemplate + MyBatis-Plus 3.5.7 |
| 数据库 | PostgreSQL 15 |
| 认证授权 | JWT + RBAC |
| 部署 | Docker Compose |

## 关键业务规则

1. **完整链路**：工单、批次、检验、检验项、不良和状态事件均通过批次关联，`GET /api/trace/{batchNo}` 返回追溯树和时间线。
2. **并发检验不覆盖**：提交检验时在数据库事务内锁定批次行；新检验记录读取并保存当时的 `previousBatchStatus` 与 `receivedBatchQuantity`，再追加批次状态事件。多个检验按严重等级聚合，后来的 PASS 不会覆盖更早的 FAIL/BLOCKED。
3. **检验原子写入**：检验主表和全部检验项在同一个事务内提交；中途失败时整体回滚，不会留下半条检验。
4. **状态只追加**：`status_event` 保存每次 `from_status`、`to_status`、事件载荷、操作人和时间。实体表中的状态只作为创建快照/旧数据兜底，当前状态由事件推导。
5. **旧工单兼容**：无批次、无状态事件的历史工单仍可查询；响应以表内状态作为 fallback，不要求补批次号或历史事件。
6. **不良关闭留痕**：不良原等级、数量、原因、原始处置 OPEN 保存在 `defect_record`；每次处置和关闭都追加事件，事件载荷保留变更前后值。关闭后仍可查询原判定和完整变更链。

## API 清单

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 公开 | 登录获取 JWT |
| POST | `/api/work-orders` | 产线主管/质量经理 | 创建工单 |
| GET | `/api/work-orders` | 登录用户 | 工单列表 |
| GET | `/api/work-orders/{id}` | 登录用户 | 工单详情 |
| GET | `/api/work-orders/order/{orderNo}` | 登录用户 | 按工单号查询，兼容旧工单 |
| POST | `/api/work-orders/{id}/start` | 产线主管/质量经理 | 开工 |
| POST | `/api/work-orders/{id}/pause` | 产线主管/质量经理 | 暂停 |
| POST | `/api/work-orders/{id}/finish` | 产线主管/质量经理 | 完工 |
| POST | `/api/work-orders/{id}/cancel` | 产线主管/质量经理 | 取消 |
| POST | `/api/batches` | 产线主管 | 批次投料 |
| GET | `/api/batches/{batchNo}` | 登录用户 | 批次详情 |
| GET | `/api/batches/{batchNo}/events` | 登录用户 | 批次状态事件 |
| POST | `/api/inspections` | 质检员/质量经理 | 原子提交检验及检验项 |
| GET | `/api/inspections/{id}` | 登录用户 | 检验详情 |
| GET | `/api/inspections/{id}/items` | 登录用户 | 检验项列表 |
| POST | `/api/defects` | 质检员/质量经理 | 登记不良 |
| POST | `/api/defects/{id}/disposition` | 质量经理 | 选择处置结论 |
| POST | `/api/defects/{id}/close` | 质量经理 | 关闭不良 |
| GET | `/api/defects/{id}/events` | 登录用户 | 不良处置历史 |
| GET | `/api/trace/{batchNo}` | 全部登录角色 | 批次全链路追溯 |

## 种子账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `inspector` | `inspector123` | QUALITY_INSPECTOR |
| `supervisor` | `supervisor123` | LINE_SUPERVISOR |
| `manager` | `manager123` | QUALITY_MANAGER |
| `auditor` | `auditor123` | AUDITOR |

## 项目目录结构

```text
backend/src/main/java/com/generated/qualityTrace/
├── routes/               # 路径常量
├── controllers/          # REST 控制器
├── services/             # 工单、批次、检验、不良、追溯等业务逻辑
├── models/               # 领域模型
├── repositories/         # JDBC 数据访问
├── middlewares/          # JWT、RBAC、限流、审计、全局异常处理
├── constants/            # 枚举、错误码、日志模板、路径常量
├── constructors/         # 响应 DTO 构造器
├── validators/           # 枚举与检验规则校验
├── exceptions/           # API 与分层异常
├── utils/                # JSON 与格式化工具
├── types/                # 请求 DTO 和登录上下文
└── config/               # MVC 拦截器配置
```

## 环境变量说明

- `COMPOSE_PROJECT_NAME`：Compose 项目名，默认 `quality-trace`。
- `BACKEND_PORT`：后端宿主机端口，默认 `21114`。
- `DB_PORT`：PostgreSQL 宿主机端口，默认 `54320`。
- `DB_NAME/DB_USER/DB_PASSWORD`：数据库名、用户和密码。
- `JWT_SECRET`：JWT HMAC 密钥，生产环境必须修改。
- `JWT_TTL_SECONDS`：JWT 有效期，默认 28800 秒。
- `RATE_LIMIT_PER_MINUTE`：同一 IP + 方法 + 路径的限流阈值。
- `LOG_LEVEL`：应用日志级别。

## Docker 部署说明

- 根目录 `docker-compose.yml` 顶层 `name: quality-trace`，不使用已废弃的 `version:`。
- `backend` 和 `db` 均使用 `${COMPOSE_PROJECT_NAME:-quality-trace}` 前缀容器名。
- 后端容器内部监听 `8080`，宿主机映射 `${BACKEND_PORT:-21114}:8080`。
- 数据库使用命名卷 `db_data`，避免绑定中文路径。
- `db` 配置 healthcheck，`backend` 通过 `depends_on: condition: service_healthy` 等待数据库就绪。
- 端口占用：修改 `.env` 的 `BACKEND_PORT` 或 `DB_PORT` 后重启。
- 重置数据：`docker compose down -v && docker compose up -d`。

## 枚举/常量出现位置清单

| 枚举 | 常量 | Model | DTO/Factory | Validator | Service | Controller | 日志/错误 |
|---|---|---|---|---|---|---|---|
| WorkOrderStatus | `constants/WorkOrderStatus.java` | `models/WorkOrder.java` | `constructors/WorkOrderDtoFactory.java` | `validators/EnumValidator.java` | `services/WorkOrderService.java` | `controllers/WorkOrderController.java` | `LogTemplates`、`ErrorMessages` |
| BatchStatus | `constants/BatchStatus.java` | `models/ProductBatch.java` | `constructors/ProductBatchDtoFactory.java`、`StatusEventDtoFactory.java` | 状态推导校验 | `ProductBatchService`、`QualityInspectionService`、`DefectRecordService` | 批次/追溯 Controller | 批次日志模板、状态事件 |
| InspectionResultStatus | `constants/InspectionResultStatus.java` | `QualityInspection`、`InspectionItemResult` | `QualityInspectionDtoFactory`、`InspectionItemResultDtoFactory` | `validators/InspectionValidator.java` | `QualityInspectionService.java` | `QualityInspectionController.java` | `LogTemplates.INSPECTION_*`、错误消息 |
| DefectSeverity | `constants/DefectSeverity.java` | `models/DefectRecord.java` | `DefectRecordDtoFactory.java` | `DefectRecordService` 枚举校验 | `DefectRecordService.java` | `DefectRecordController.java` | 不良登记日志模板 |
| DefectDispositionStatus | `constants/DefectDispositionStatus.java` | `models/DefectRecord.java` | `DefectRecordDtoFactory.java`、`StatusEventDtoFactory.java` | `EnumValidator` | `DefectRecordService.java` | `DefectRecordController.java` | 处置变更日志和错误消息 |
| UserRole | `constants/UserRole.java` | `models/User.java` | 登录响应 | `RequireRoles` | `AuthService` | 全部受保护 Controller | RBAC 错误码/消息 |

## 为什么会牵一发动全身

状态枚举同时影响数据库 CHECK 约束、请求校验、服务状态推导、响应构造、日志模板、权限注解和追溯时间线；状态存储采用追加事件而非单点覆盖，因此新增一个业务动作通常需要同时修改常量、事件服务、实体服务、追溯 DTO、README 和测试流程。这种分层拆分保证审计和状态历史一致，但也要求跨文件协同改动。

## License

MIT
