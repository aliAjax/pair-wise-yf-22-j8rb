package com.generated.qualityTrace.types;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 不良登记请求。 */
public class DefectRecordPayload {

    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    @NotBlank(message = "不良类型不能为空")
    private String defectType;

    @NotNull
    @Min(value = 1, message = "不良数量必须大于 0")
    private Integer defectQty;

    @NotBlank(message = "严重度不能为空（MINOR/MAJOR/CRITICAL）")
    private String severity;

    @NotBlank(message = "根本原因不能为空")
    private String rootCause;

    /** 原判定：REWORK 返工 / CONCESSION 让步接收 / SCRAP 报废；登记后不可改。 */
    @NotBlank(message = "原判定不能为空（REWORK/CONCESSION/SCRAP）")
    private String originalResult;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getDefectType() { return defectType; }
    public void setDefectType(String defectType) { this.defectType = defectType; }
    public Integer getDefectQty() { return defectQty; }
    public void setDefectQty(Integer defectQty) { this.defectQty = defectQty; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getOriginalResult() { return originalResult; }
    public void setOriginalResult(String originalResult) { this.originalResult = originalResult; }
}
