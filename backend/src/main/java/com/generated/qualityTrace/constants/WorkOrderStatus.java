package com.generated.qualityTrace.constants;

import java.util.EnumSet;
import java.util.Set;

/**
 * 工单状态。当前状态不存列，由 state_event 流重放得出，
 * 这里的 ALLOWED 仅用于校验“允许发生的状态流转”。
 */
public enum WorkOrderStatus {
    PLANNED,
    RUNNING,
    PAUSED,
    FINISHED,
    CANCELLED;

    public static final Set<WorkOrderStatus> ALLOW_START   = EnumSet.of(PLANNED, PAUSED);
    public static final Set<WorkOrderStatus> ALLOW_PAUSE   = EnumSet.of(RUNNING);
    public static final Set<WorkOrderStatus> ALLOW_FINISH  = EnumSet.of(RUNNING, PAUSED);
    public static final Set<WorkOrderStatus> ALLOW_CANCEL  = EnumSet.of(PLANNED, RUNNING, PAUSED);

    public String code() {
        return name();
    }
}
