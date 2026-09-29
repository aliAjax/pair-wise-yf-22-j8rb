package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/** 质量检验主表。 */
@TableName("quality_inspection")
public class QualityInspection {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long inspectorId;
    private String inspectionType;   // FIRST / PATROL / FINAL
    private String standardVersion;
    private String resultStatus;     // PASS / FAIL / CONDITIONAL_PASS / RECHECK
    private OffsetDateTime inspectedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
    public String getStandardVersion() { return standardVersion; }
    public void setStandardVersion(String standardVersion) { this.standardVersion = standardVersion; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public OffsetDateTime getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(OffsetDateTime inspectedAt) { this.inspectedAt = inspectedAt; }
}
