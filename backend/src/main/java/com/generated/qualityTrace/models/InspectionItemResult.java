package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/** 检验项结果。 */
@TableName("inspection_item_result")
public class InspectionItemResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private String itemCode;
    private String itemName;
    private BigDecimal measuredValue;
    private BigDecimal limitMin;
    private BigDecimal limitMax;
    private String itemStatus;       // PASS / FAIL / RECHECK

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInspectionId() { return inspectionId; }
    public void setInspectionId(Long inspectionId) { this.inspectionId = inspectionId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(BigDecimal measuredValue) { this.measuredValue = measuredValue; }
    public BigDecimal getLimitMin() { return limitMin; }
    public void setLimitMin(BigDecimal limitMin) { this.limitMin = limitMin; }
    public BigDecimal getLimitMax() { return limitMax; }
    public void setLimitMax(BigDecimal limitMax) { this.limitMax = limitMax; }
    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }
}
