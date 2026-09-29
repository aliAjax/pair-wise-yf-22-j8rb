package com.generated.qualityTrace.constants;

/** 检验结论：合格 / 不合格 / 让步接收 / 复检。 */
public enum InspectionResultStatus {
    PASS,
    FAIL,
    CONDITIONAL_PASS,
    RECHECK;

    public boolean blocksBatch() {
        // FAIL 直接冻结批次；RECHECK 挂起待复检
        return this == FAIL || this == RECHECK;
    }

    public String code() {
        return name();
    }
}
