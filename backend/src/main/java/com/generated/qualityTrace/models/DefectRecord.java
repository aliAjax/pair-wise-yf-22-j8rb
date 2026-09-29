package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 不良记录。
 * originalResult / originalSeverity 是登记时的“原判定”，
 * 之后任何处置（返工/让步/报废）只追加 state_event，
 * 即使关闭，原判定与每次变更前后值仍可追溯。
 * 不设 dispositionStatus 可变列——由 state_event 重放。
 */
@TableName("defect_record")
public class DefectRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String defectType;
    private Integer defectQty;
    private String severity;          // MINOR / MAJOR / CRITICAL（原严重度，不改）
    private String rootCause;
    private String originalResult;    // REWORK / CONCESSION / SCRAP（原判定，不改）
    private String originalSeverity;  // 与 severity 一致冗余保存，供关闭后展示
    private String createdBy;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
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
    public String getOriginalSeverity() { return originalSeverity; }
    public void setOriginalSeverity(String originalSeverity) { this.originalSeverity = originalSeverity; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
