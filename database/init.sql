-- ============================================================
-- quality-trace 制造业质量追溯 数据库初始化
-- 设计要点：
--   1. 状态不落可变列：work_order / product_batch 不保存可覆盖的 status，
--      当前状态统一由 state_event 事件流重放得到（事件溯源）。
--   2. 每次状态变化追加一条 state_event，含 from_state/to_state 与变更摘要。
--   3. 检验/不良处置整批写入，靠事务保证“不留半条记录”。
--   4. product_batch.work_order_id 可空，兼容无批次号时代的旧工单。
-- ============================================================

DROP TABLE IF EXISTS inspection_item_result CASCADE;
DROP TABLE IF EXISTS quality_inspection CASCADE;
DROP TABLE IF EXISTS defect_record CASCADE;
DROP TABLE IF EXISTS product_batch CASCADE;
DROP TABLE IF EXISTS work_order CASCADE;
DROP TABLE IF EXISTS state_event CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;

-- ---------- 用户与角色（JWT + RBAC） ----------
CREATE TABLE app_user (
  id              BIGSERIAL PRIMARY KEY,
  username        TEXT NOT NULL UNIQUE,
  display_name    TEXT NOT NULL,
  password_hash   TEXT NOT NULL,
  password_salt   TEXT NOT NULL,
  role            TEXT NOT NULL CHECK (role IN ('INSPECTOR','SUPERVISOR','MANAGER','AUDITOR')),
  enabled         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- 生产工单 ----------
CREATE TABLE work_order (
  id              BIGSERIAL PRIMARY KEY,
  order_no        TEXT NOT NULL UNIQUE,
  product_code    TEXT NOT NULL,
  product_name    TEXT NOT NULL,
  planned_qty     INTEGER NOT NULL CHECK (planned_qty > 0),
  completed_qty   INTEGER NOT NULL DEFAULT 0,
  line_code       TEXT NOT NULL,
  start_at        TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
  -- 注意：没有 status 列。当前状态由 state_event 最新一条推出。
);

-- ---------- 产品批次（work_order_id 可空：旧工单可无批次） ----------
CREATE TABLE product_batch (
  id              BIGSERIAL PRIMARY KEY,
  batch_no        TEXT NOT NULL UNIQUE,
  work_order_id   BIGINT REFERENCES work_order(id),
  quantity        INTEGER NOT NULL CHECK (quantity > 0),
  qualified_qty   INTEGER NOT NULL DEFAULT 0,
  defect_qty      INTEGER NOT NULL DEFAULT 0,
  material_lot_no TEXT NOT NULL,
  produced_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
  -- 注意：没有 batch_status 列。当前批次状态由 state_event 推出。
);
CREATE INDEX idx_product_batch_work_order ON product_batch(work_order_id);

-- ---------- 质量检验 ----------
CREATE TABLE quality_inspection (
  id                BIGSERIAL PRIMARY KEY,
  batch_id          BIGINT NOT NULL REFERENCES product_batch(id),
  inspector_id      BIGINT NOT NULL REFERENCES app_user(id),
  inspection_type   TEXT NOT NULL CHECK (inspection_type IN ('FIRST','PATROL','FINAL')),
  standard_version  TEXT NOT NULL,
  result_status     TEXT NOT NULL CHECK (result_status IN ('PASS','FAIL','CONDITIONAL_PASS','RECHECK')),
  inspected_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inspection_batch ON quality_inspection(batch_id);

-- ---------- 检验项结果 ----------
CREATE TABLE inspection_item_result (
  id              BIGSERIAL PRIMARY KEY,
  inspection_id   BIGINT NOT NULL REFERENCES quality_inspection(id) ON DELETE CASCADE,
  item_code       TEXT NOT NULL,
  item_name       TEXT NOT NULL,
  measured_value  NUMERIC(14,4) NOT NULL,
  limit_min       NUMERIC(14,4) NOT NULL,
  limit_max       NUMERIC(14,4) NOT NULL,
  item_status     TEXT NOT NULL CHECK (item_status IN ('PASS','FAIL','RECHECK'))
);
CREATE INDEX idx_item_inspection ON inspection_item_result(inspection_id);

-- ---------- 不良记录 ----------
CREATE TABLE defect_record (
  id                  BIGSERIAL PRIMARY KEY,
  batch_id            BIGINT NOT NULL REFERENCES product_batch(id),
  defect_type         TEXT NOT NULL,
  defect_qty          INTEGER NOT NULL CHECK (defect_qty > 0),
  severity            TEXT NOT NULL CHECK (severity IN ('MINOR','MAJOR','CRITICAL')),
  root_cause          TEXT NOT NULL,
  original_result     TEXT NOT NULL,          -- 原判定，登记后不可改
  original_severity   TEXT NOT NULL,          -- 原严重度，登记后不可改
  created_by          TEXT NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
  -- 处置状态同样不设可变列：由 state_event (DISPOSITION_*) 推出。
);
CREATE INDEX idx_defect_batch ON defect_record(batch_id);

-- ---------- 状态事件流（事件溯源，只追加） ----------
CREATE TABLE state_event (
  id                BIGSERIAL PRIMARY KEY,
  aggregate_type    TEXT NOT NULL CHECK (aggregate_type IN ('WORK_ORDER','BATCH','DEFECT')),
  aggregate_id      BIGINT NOT NULL,
  event_type        TEXT NOT NULL,
  from_state        TEXT,
  to_state          TEXT,
  change_summary    JSONB,
  actor             TEXT,
  idempotency_key   TEXT,
  occurred_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- 重放与追溯的核心索引：按聚合并发串行、按时间排序
CREATE INDEX idx_state_event_agg ON state_event(aggregate_type, aggregate_id, id);
CREATE INDEX idx_state_event_occurred ON state_event(occurred_at);
-- 幂等键按聚合唯一：同一批次重复回传不产生第二条
CREATE UNIQUE INDEX uq_state_event_idem
  ON state_event(aggregate_type, aggregate_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

-- ---------- 操作审计日志（中间件落库） ----------
CREATE TABLE audit_log (
  id              BIGSERIAL PRIMARY KEY,
  actor           TEXT,
  role            TEXT,
  action          TEXT NOT NULL,
  method          TEXT,
  path            TEXT,
  target_type     TEXT,
  target_id       TEXT,
  detail          TEXT,
  result_status   INTEGER,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_target ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

-- ============================================================
-- 种子数据
-- ============================================================

-- 密码 = sha256(salt + 明文)，明文见 README
INSERT INTO app_user (username, display_name, password_hash, password_salt, role) VALUES
 ('inspector', '质检员甲', '41868fb24c8a6152926d9b33d4d6d586a432119c177b9eaa99e8558e7ce65560', '8e798493d8c74a7e', 'INSPECTOR'),
 ('supervisor','产线主管乙','bf3ec119ff4df72005fe614fa354191fbc99a7bdc137dd0373e165e4367609b7', '8f413027f4b1cf90', 'SUPERVISOR'),
 ('manager',   '质量经理丙', '492bcb59ddc4854f15b39dc7585fe89136b01832e7824c43581f42f345f29286', '8786cc3bc23b1ddd', 'MANAGER'),
 ('auditor',   '审计员丁',  '494ebfa8ddb5668800ca2722c461b0ff5380d49c553d8c5c7250186f5d7ae547', 'b6a8c50670bcb981', 'AUDITOR');

-- 旧工单：早于批次管理上线，没有任何批次，但仍应可查询
INSERT INTO work_order (id, order_no, product_code, product_name, planned_qty, completed_qty, line_code, start_at, created_at)
VALUES (1001, 'WO-2025-LEGACY-01', 'P-OLD-01', '旧式电机壳', 500, 0, 'L-OLD', NULL, TIMESTAMPTZ '2025-11-01 08:00:00+08');
SELECT setval(pg_get_serial_sequence('work_order','id'), 1001);

-- 正常工单 + 批次 + 检验 + 不良 的样例链路
INSERT INTO work_order (id, order_no, product_code, product_name, planned_qty, completed_qty, line_code, start_at, created_at)
VALUES (2001, 'WO-2026-0901-01', 'P-GEAR-07', '7号传动齿轮', 1000, 200, 'L-A1',
        TIMESTAMPTZ '2026-09-28 08:30:00+08', TIMESTAMPTZ '2026-09-28 08:00:00+08');
SELECT setval(pg_get_serial_sequence('work_order','id'), 2001);

INSERT INTO product_batch (id, batch_no, work_order_id, quantity, qualified_qty, defect_qty, material_lot_no, produced_at)
VALUES (3001, 'B2026092801', 2001, 200, 198, 2, 'MAT-20260925-A',
        TIMESTAMPTZ '2026-09-28 10:00:00+08');
SELECT setval(pg_get_serial_sequence('product_batch','id'), 3001);

INSERT INTO quality_inspection (id, batch_id, inspector_id, inspection_type, standard_version, result_status, inspected_at)
VALUES (4001, 3001, 1, 'FIRST', 'STD-GEAR-V3.2', 'PASS', TIMESTAMPTZ '2026-09-28 10:20:00+08');
SELECT setval(pg_get_serial_sequence('quality_inspection','id'), 4001);

INSERT INTO inspection_item_result (inspection_id, item_code, item_name, measured_value, limit_min, limit_max, item_status) VALUES
 (4001, 'DIM-01', '齿顶圆直径', 49.98, 49.90, 50.10, 'PASS'),
 (4001, 'DIM-02', '同轴度',     0.03,  0.00,  0.05,  'PASS'),
 (4001, 'SFC-01', '表面粗糙度', 1.6,   0.0,   3.2,   'PASS');

INSERT INTO defect_record (id, batch_id, defect_type, defect_qty, severity, root_cause,
                           original_result, original_severity, created_by, created_at)
VALUES (5001, 3001, '齿面磕碰伤', 2, 'MINOR', '转运工装缓冲垫老化',
        'SCRAP', 'MINOR', 'inspector', TIMESTAMPTZ '2026-09-28 11:00:00+08');
SELECT setval(pg_get_serial_sequence('defect_record','id'), 5001);

-- 与种子数据对应的事件流（当前状态可由它们重放）
INSERT INTO state_event (aggregate_type, aggregate_id, event_type, from_state, to_state, change_summary, actor, occurred_at) VALUES
 ('WORK_ORDER', 1001, 'ORDER_CREATED',  NULL,      'PLANNED', '{"orderNo":"WO-2025-LEGACY-01","note":"历史迁移工单，无批次"}', NULL,              TIMESTAMPTZ '2025-11-01 08:00:00+08'),
 ('WORK_ORDER', 2001, 'ORDER_CREATED',  NULL,      'PLANNED', '{"orderNo":"WO-2026-0901-01"}',                                   'supervisor',      TIMESTAMPTZ '2026-09-28 08:00:00+08'),
 ('WORK_ORDER', 2001, 'ORDER_STARTED',  'PLANNED', 'RUNNING', '{"lineCode":"L-A1"}',                                            'supervisor',      TIMESTAMPTZ '2026-09-28 08:30:00+08'),
 ('BATCH', 3001, 'BATCH_CREATED',     NULL,      'PRODUCED',  '{"batchNo":"B2026092801","quantity":200,"workOrderId":2001}',   'supervisor',      TIMESTAMPTZ '2026-09-28 10:00:00+08'),
 ('BATCH', 3001, 'INSPECTION_PASSED', 'PRODUCED','INSPECTED_PASS','{"inspectionId":4001,"inspectionType":"FIRST","qualifiedQty":200}', 'inspector', TIMESTAMPTZ '2026-09-28 10:20:00+08'),
 ('BATCH', 3001, 'DEFECT_REGISTERED', 'INSPECTED_PASS','BLOCKED','{"defectId":5001,"defectQty":2,"qualifiedQty":198}',           'inspector',      TIMESTAMPTZ '2026-09-28 11:00:00+08'),
 ('DEFECT', 5001, 'DEFECT_REGISTERED',  NULL,           'OPEN',     '{"originalResult":"SCRAP","severity":"MINOR","defectQty":2}','inspector',      TIMESTAMPTZ '2026-09-28 11:00:00+08');
SELECT setval(pg_get_serial_sequence('state_event','id'), 6001);
