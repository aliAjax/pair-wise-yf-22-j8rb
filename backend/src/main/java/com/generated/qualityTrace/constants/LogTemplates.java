package com.generated.qualityTrace.constants;

public final class LogTemplates {
  private LogTemplates() {}

  public static final String USER_LOGIN = "用户 %s 登录系统";
  public static final String WORK_ORDER_CREATED = "创建工单 %s，产品 %s，计划数量 %s";
  public static final String WORK_ORDER_STATUS_CHANGED = "工单 %s 状态从 %s 变更为 %s";
  public static final String BATCH_CREATED = "批次 %s 投料至工单 %s，数量 %s，物料批 %s";
  public static final String BATCH_STATUS_CHANGED = "批次 %s 状态从 %s 变更为 %s，事件 %s";
  public static final String INSPECTION_CREATED = "批次 %s 录入%s检验，结论 %s，承接数量 %s，前状态 %s";
  public static final String INSPECTION_ITEM_SAVED = "检验 %s 写入检验项 %s，判定 %s";
  public static final String DEFECT_REGISTERED = "批次 %s 登记不良 %s，等级 %s，数量 %s";
  public static final String DEFECT_DISPOSITION_CHANGED = "不良 %s 处置从 %s 变更为 %s";
  public static final String DEFECT_CLOSED = "不良 %s 已关闭，原判定 %s，最终处置 %s";
}
