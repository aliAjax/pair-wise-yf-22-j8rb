package com.generated.qualityTrace.models;

import java.math.BigDecimal;
import java.time.Instant;

public record InspectionItemResult(
    Long id,
    Long inspectionId,
    String itemCode,
    String itemName,
    BigDecimal measuredValue,
    BigDecimal limitMin,
    BigDecimal limitMax,
    String itemStatus,
    Instant createdAt
) {}
