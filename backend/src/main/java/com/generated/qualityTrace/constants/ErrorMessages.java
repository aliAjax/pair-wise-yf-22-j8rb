package com.generated.qualityTrace.constants;

public final class ErrorMessages {
  private ErrorMessages() {}

  public static final String AUTH_REQUIRED = "缺少 Bearer Token";
  public static final String INVALID_TOKEN = "Token 无效或已过期";
  public static final String BAD_CREDENTIALS = "用户名或密码错误";
  public static final String RBAC_DENIED = "当前角色无权执行该操作";
  public static final String RATE_LIMITED = "请求过于频繁，请稍后再试";
  public static final String WORK_ORDER_NOT_FOUND = "工单不存在：%s";
  public static final String BATCH_NOT_FOUND = "批次不存在：%s";
  public static final String INSPECTION_NOT_FOUND = "检验记录不存在：%s";
  public static final String DEFECT_NOT_FOUND = "不良记录不存在：%s";
  public static final String DUPLICATE_BATCH = "批次号已存在：%s";
  public static final String DUPLICATE_WORK_ORDER = "工单号已存在：%s";
  public static final String ILLEGAL_TRANSITION = "不能将状态从 %s 变更为 %s";
  public static final String WORK_ORDER_NOT_RUNNING = "只有 RUNNING 工单可以投料";
  public static final String PLANNED_QTY_EXCEEDED = "投料累计数量 %s 已超过工单计划数量 %s";
  public static final String FINISH_QTY_NOT_ENOUGH = "完工数量 %s 未达到计划数量 %s";
  public static final String DEFECT_ALREADY_CLOSED = "不良记录已关闭，不能继续处置";
  public static final String DEFECT_MUST_DISPOSE_BEFORE_CLOSE = "关闭前必须先明确处置结论";
  public static final String DEFECT_QTY_EXCEEDS_BATCH = "不良数量 %s 超过批次当前数量 %s";
  public static final String ITEMS_REQUIRED = "检验至少需要一个检验项";
  public static final String CONFLICTING_RESULT = "提交结论 %s 与检验项汇总结果 %s 不一致";
}
