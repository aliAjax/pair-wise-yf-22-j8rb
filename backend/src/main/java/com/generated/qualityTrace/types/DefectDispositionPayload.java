package com.generated.qualityTrace.types;

import jakarta.validation.constraints.NotBlank;

/**
 * 不良处置变更请求。只能推动处置状态，不能改原判定/数量/严重度。
 * targetStatus: IN_DISPOSITION -> REWORKING / CONCESSION / SCRAPPED / CLOSED
 */
public class DefectDispositionPayload {

    @NotBlank(message = "目标处置状态不能为空")
    private String targetStatus;

    /** 处置说明（关闭时必填关闭结论）。 */
    private String dispositionNote;

    public String getTargetStatus() { return targetStatus; }
    public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }
    public String getDispositionNote() { return dispositionNote; }
    public void setDispositionNote(String dispositionNote) { this.dispositionNote = dispositionNote; }
}
