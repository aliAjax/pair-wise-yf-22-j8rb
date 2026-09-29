package com.generated.qualityTrace.constants;

/**
 * 日志模板集中登记。所有写操作都必须记录日志；
 * 字段变更时必须同步修改模板与调用处。
 * 每个实体不少于 4 条。
 */
public final class LogTemplates {
    private LogTemplates() {}

    // ---- 工单（4+） ----
    public static final String WO_CREATE  = "[WORK_ORDER] 创建工单 orderNo=%s productCode=%s plannedQty=%s actor=%s";
    public static final String WO_START   = "[WORK_ORDER] 工单开工 orderNo=%s state %s -> %s line=%s actor=%s";
    public static final String WO_PAUSE   = "[WORK_ORDER] 工单暂停 orderNo=%s state %s -> %s actor=%s";
    public static final String WO_FINISH  = "[WORK_ORDER] 工单完工 orderNo=%s state %s -> %s completedQty=%s actor=%s";
    public static final String WO_CANCEL  = "[WORK_ORDER] 工单取消 orderNo=%s state %s -> %s actor=%s";
    public static final String WO_PROGRESS= "[WORK_ORDER] 工单进度回写 orderNo=%s completedQty %s -> %s actor=%s";

    // ---- 批次（4+） ----
    public static final String BATCH_CREATE   = "[BATCH] 创建批次 batchNo=%s workOrderId=%s quantity=%s materialLot=%s actor=%s";
    public static final String BATCH_INSPECT  = "[BATCH] 批次检验回传 batchNo=%s type=%s result=%s 承接前态=%s/合格=%s actor=%s";
    public static final String BATCH_QUANTITY = "[BATCH] 批次数量接续 batchNo=%s qualified %s -> %s defect %s -> %s actor=%s";
    public static final String BATCH_RELEASE  = "[BATCH] 批次放行 batchNo=%s state %s -> %s actor=%s";
    public static final String BATCH_QUARANTINE = "[BATCH] 批次隔离 batchNo=%s state %s -> %s actor=%s";

    // ---- 检验（4+） ----
    public static final String INS_SUBMIT  = "[INSPECTION] 提交检验 batchNo=%s type=%s std=%s items=%s result=%s actor=%s";
    public static final String INS_ITEM    = "[INSPECTION] 检验项录入 inspectionId=%s item=%s measured=%s[%s,%s] status=%s";
    public static final String INS_REPLAY  = "[INSPECTION] 幂等回放 inspectionId=%s key=%s actor=%s";
    public static final String INS_FAIL_TX = "[INSPECTION] 检验整批回滚 batchNo=%s items=%s reason=%s actor=%s";

    // ---- 不良（4+） ----
    public static final String DEF_REGISTER = "[DEFECT] 登记不良 batchNo=%s type=%s qty=%s severity=%s 原判定=%s actor=%s";
    public static final String DEF_DISPOSE  = "[DEFECT] 不良处置变更 defectId=%s %s -> %s before=%s after=%s actor=%s";
    public static final String DEF_CLOSE    = "[DEFECT] 不良处置关闭 defectId=%s final=%s 原判定=%s severity=%s actor=%s";
    public static final String DEF_REJECT   = "[DEFECT] 不良处置被拒 defectId=%s current=%s attempted=%s actor=%s";

    // ---- 追溯/系统 ----
    public static final String TRACE_QUERY = "[TRACE] 全链路追溯查询 batchNo=%s events=%s inspections=%s defects=%s actor=%s";
}
