package com.generated.qualityTrace.types;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 检验项结果录入。 */
public class InspectionItemResultPayload {

    @NotBlank(message = "检验项编码不能为空")
    private String itemCode;

    @NotBlank(message = "检验项名称不能为空")
    private String itemName;

    @NotNull(message = "实测值不能为空")
    private BigDecimal measuredValue;

    @NotNull(message = "下限不能为空")
    private BigDecimal limitMin;

    @NotNull(message = "上限不能为空")
    private BigDecimal limitMax;

    /** 可空：服务端按 measuredValue 是否落在 [limitMin, limitMax] 判定。 */
    private String itemStatus;

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
