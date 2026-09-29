package com.generated.qualityTrace.models;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductBatch(
    Long id,
    String batchNo,
    Long workOrderId,
    BigDecimal quantity,
    String materialLotNo,
    Instant producedAt,
    String batchStatus,
    Instant createdAt,
    Instant updatedAt
) {}
