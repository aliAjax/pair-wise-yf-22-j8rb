package com.generated.qualityTrace.types;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WorkOrderPayload(
    @NotBlank String orderNo,
    @NotBlank String productCode,
    @NotBlank String productName,
    @NotNull @Positive BigDecimal plannedQty,
    @NotBlank String lineCode
) {}
