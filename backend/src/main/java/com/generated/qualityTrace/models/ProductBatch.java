package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 产品批次。workOrderId 可空（兼容无批次号时代的旧数据反向场景：
 * 旧工单没有批次，查询工单时不强制回填）。
 * 注意：没有 batchStatus 字段——由 state_event 重放。
 */
@TableName("product_batch")
public class ProductBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private Long workOrderId;
    private Integer quantity;
    private Integer qualifiedQty;
    private Integer defectQty;
    private String materialLotNo;
    private OffsetDateTime producedAt;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(Integer qualifiedQty) { this.qualifiedQty = qualifiedQty; }
    public Integer getDefectQty() { return defectQty; }
    public void setDefectQty(Integer defectQty) { this.defectQty = defectQty; }
    public String getMaterialLotNo() { return materialLotNo; }
    public void setMaterialLotNo(String materialLotNo) { this.materialLotNo = materialLotNo; }
    public OffsetDateTime getProducedAt() { return producedAt; }
    public void setProducedAt(OffsetDateTime producedAt) { this.producedAt = producedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
