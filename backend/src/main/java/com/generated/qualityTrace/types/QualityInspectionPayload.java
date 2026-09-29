package com.generated.qualityTrace.types;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 检验录入请求。多条检验可同时回传同一批次：
 * 服务端在事务内锁批次行、重放事件接续前态，调用方不需要也不能传当前状态。
 */
public class QualityInspectionPayload {

    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    @NotBlank(message = "检验类型不能为空（FIRST/PATROL/FINAL）")
    private String inspectionType;

    @NotBlank(message = "检验标准版本不能为空")
    private String standardVersion;

    /**
     * 本批检验覆盖的合格/复检/不良数量。后到的检验在此前累计值上接续，不覆盖。
     * qualifiedQty + recheckQty 对应本次确认数量，defectQty 对应本次不良数。
     */
    private Integer qualifiedQty;
    private Integer recheckQty;
    private Integer defectQty;

    @NotEmpty(message = "至少提交一个检验项")
    @Valid
    private List<InspectionItemResultPayload> items;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
    public String getStandardVersion() { return standardVersion; }
    public void setStandardVersion(String standardVersion) { this.standardVersion = standardVersion; }
    public Integer getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(Integer qualifiedQty) { this.qualifiedQty = qualifiedQty; }
    public Integer getRecheckQty() { return recheckQty; }
    public void setRecheckQty(Integer recheckQty) { this.recheckQty = recheckQty; }
    public Integer getDefectQty() { return defectQty; }
    public void setDefectQty(Integer defectQty) { this.defectQty = defectQty; }
    public List<InspectionItemResultPayload> getItems() { return items; }
    public void setItems(List<InspectionItemResultPayload> items) { this.items = items; }
}
