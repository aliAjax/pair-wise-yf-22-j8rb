package com.generated.qualityTrace.types;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InspectionItemPayload(
    @NotBlank String itemCode,
    @NotBlank String itemName,
    @NotNull BigDecimal measuredValue,
    @NotNull BigDecimal limitMin,
    @NotNull BigDecimal limitMax,
    String itemStatus
) {}
