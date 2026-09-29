package com.generated.qualityTrace.models;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkOrder(
    Long id,
    String orderNo,
    String productCode,
    String productName,
    BigDecimal plannedQty,
    String lineCode,
    Instant startAt,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
