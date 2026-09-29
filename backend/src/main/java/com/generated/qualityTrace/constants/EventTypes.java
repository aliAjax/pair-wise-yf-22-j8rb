package com.generated.qualityTrace.constants;

/**
 * 事件类型常量（state_event.event_type）。
 * 状态变化只追加、不覆盖，所有状态流转事件集中登记在此。
 */
public final class EventTypes {
    private EventTypes() {}

    // 工单
    public static final String ORDER_CREATED         = "ORDER_CREATED";
    public static final String ORDER_STARTED         = "ORDER_STARTED";
    public static final String ORDER_PAUSED          = "ORDER_PAUSED";
    public static final String ORDER_FINISHED        = "ORDER_FINISHED";
    public static final String ORDER_CANCELLED       = "ORDER_CANCELLED";
    public static final String ORDER_PROGRESS_ADDED  = "ORDER_PROGRESS_ADDED";

    // 批次
    public static final String BATCH_CREATED           = "BATCH_CREATED";
    public static final String INSPECTION_PASSED       = "INSPECTION_PASSED";
    public static final String INSPECTION_FAILED       = "INSPECTION_FAILED";
    public static final String INSPECTION_CONDITIONAL  = "INSPECTION_CONDITIONAL";
    public static final String INSPECTION_RECHECK      = "INSPECTION_RECHECK";
    public static final String DEFECT_REGISTERED       = "DEFECT_REGISTERED";
    public static final String BATCH_RELEASED          = "BATCH_RELEASED";
    public static final String BATCH_QUARANTINED       = "BATCH_QUARANTINED";

    // 不良处置
    public static final String DEFECT_DISPOSITION_CHANGED = "DEFECT_DISPOSITION_CHANGED";
    public static final String DEFECT_CLOSED              = "DEFECT_CLOSED";
}
