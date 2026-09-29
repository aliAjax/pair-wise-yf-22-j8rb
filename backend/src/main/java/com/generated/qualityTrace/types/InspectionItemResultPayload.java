package com.generated.qualityTrace.types;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record InspectionItemResultPayload(
    Long inspectionId,
    String itemCode,
    String itemName,
    @NotNull BigDecimal measuredValue,
    @NotNull BigDecimal limitMin,
    @NotNull BigDecimal limitMax,
    String itemStatus
) {}
