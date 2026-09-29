package com.generated.qualityTrace.constants;

/** 错误消息模板集中登记，参数用 %s 占位。 */
public final class ErrorMessages {
    private ErrorMessages() {}

    public static final String AUTH_REQUIRED       = "缺少认证令牌，请先登录";
    public static final String AUTH_INVALID_TOKEN  = "认证令牌无效或已过期";
    public static final String RBAC_DENIED        = "当前角色 %s 无权执行 %s";
    public static final String RATE_LIMITED       = "请求过于频繁，每分钟限额 %s 次";

    public static final String WORK_ORDER_NOT_FOUND  = "工单不存在：%s";
    public static final String WORK_ORDER_CONFLICT   = "工单 %s 当前状态 %s，不允许执行 %s";
    public static final String BATCH_NOT_FOUND       = "批次不存在：%s";
    public static final String BATCH_NO_CONFLICT     = "批次号已存在：%s";
    public static final String BATCH_STATE_CONFLICT  = "批次 %s 当前状态 %s，不允许提交检验";
    public static final String INSPECTION_EMPTY      = "检验 %s 至少需要一个检验项";
    public static final String INSPECTION_CONFLICT   = "检验项 %s 的实测值缺失或不在数值范围内";
    public static final String DEFECT_NOT_FOUND      = "不良记录不存在：%s";
    public static final String DEFECT_ALREADY_CLOSED = "不良记录 %s 已关闭，禁止再变更（原判定保留）";
    public static final String DEFECT_QTY_EXCEEDED   = "不良数量 %s 超过批次可计不良余量 %s";
    public static final String DEFECT_TRANSITION_DENIED = "不良处置不允许从 %s 变更为 %s";
    public static final String IDEMPOTENCY_MISMATCH  = "幂等键 %s 对应的请求内容不一致";
}
