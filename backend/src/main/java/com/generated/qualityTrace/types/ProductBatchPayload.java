package com.generated.qualityTrace.types;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 批次投料请求。workOrderId 可空——旧工单/补录数据不强制回填批次关系。
 */
public class ProductBatchPayload {

    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    /** 可空：无批次号时代的旧工单不强制关联。 */
    private Long workOrderId;

    @NotNull(message = "批次数量不能为空")
    @Min(value = 1, message = "批次数量必须大于 0")
    private Integer quantity;

    @NotBlank(message = "物料批号不能为空")
    private String materialLotNo;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getMaterialLotNo() { return materialLotNo; }
    public void setMaterialLotNo(String materialLotNo) { this.materialLotNo = materialLotNo; }
}
