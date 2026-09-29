package com.generated.qualityTrace.constants;

/** 不良严重度。 */
public enum DefectSeverity {
    MINOR,
    MAJOR,
    CRITICAL;

    /** CRITICAL 不允许让步放行，处置流转时强校验。 */
    public boolean canConcession() {
        return this != CRITICAL;
    }

    public String code() {
        return name();
    }
}
