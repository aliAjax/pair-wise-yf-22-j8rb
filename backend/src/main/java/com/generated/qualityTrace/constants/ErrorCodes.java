package com.generated.qualityTrace.constants;

/** 错误码集中登记。service / controller 各自包装异常，不在一处吞掉。 */
public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String VALIDATION_FAILED      = "VALIDATION_FAILED";
    public static final String AUTH_REQUIRED          = "AUTH_REQUIRED";
    public static final String AUTH_INVALID_TOKEN     = "AUTH_INVALID_TOKEN";
    public static final String RBAC_DENIED            = "RBAC_DENIED";
    public static final String RATE_LIMITED           = "RATE_LIMITED";

    public static final String WORK_ORDER_NOT_FOUND   = "WORK_ORDER_NOT_FOUND";
    public static final String WORK_ORDER_CONFLICT    = "WORK_ORDER_CONFLICT";
    public static final String BATCH_NOT_FOUND        = "BATCH_NOT_FOUND";
    public static final String BATCH_NO_CONFLICT      = "BATCH_NO_CONFLICT";
    public static final String BATCH_STATE_CONFLICT   = "BATCH_STATE_CONFLICT";
    public static final String INSPECTION_EMPTY       = "INSPECTION_EMPTY";
    public static final String INSPECTION_CONFLICT    = "INSPECTION_CONFLICT";
    public static final String DEFECT_NOT_FOUND       = "DEFECT_NOT_FOUND";
    public static final String DEFECT_ALREADY_CLOSED  = "DEFECT_ALREADY_CLOSED";
    public static final String DEFECT_QTY_EXCEEDED    = "DEFECT_QTY_EXCEEDED";
    public static final String DEFECT_TRANSITION_DENIED = "DEFECT_TRANSITION_DENIED";
    public static final String IDEMPOTENCY_MISMATCH   = "IDEMPOTENCY_MISMATCH";
    public static final String INTERNAL_ERROR         = "INTERNAL_ERROR";
}
