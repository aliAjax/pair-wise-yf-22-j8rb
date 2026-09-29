package com.generated.qualityTrace.types;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 创建工单请求。 */
public class WorkOrderPayload {

    @NotBlank(message = "工单号不能为空")
    private String orderNo;

    @NotBlank(message = "产品编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{2,32}$", message = "产品编码须为 2-32 位字母数字或连字符")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量必须大于 0")
    private Integer plannedQty;

    @NotBlank(message = "产线编码不能为空")
    private String lineCode;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getPlannedQty() { return plannedQty; }
    public void setPlannedQty(Integer plannedQty) { this.plannedQty = plannedQty; }
    public String getLineCode() { return lineCode; }
    public void setLineCode(String lineCode) { this.lineCode = lineCode; }
}
