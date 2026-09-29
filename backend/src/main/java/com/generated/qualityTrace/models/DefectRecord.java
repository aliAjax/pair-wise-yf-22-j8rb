package com.generated.qualityTrace.models;

import java.math.BigDecimal;
import java.time.Instant;

public record DefectRecord(
    Long id,
    Long batchId,
    String defectType,
    BigDecimal defectQty,
    String severity,
    String rootCause,
    String originalDisposition,
    String dispositionStatus,
    Instant registeredAt,
    Instant closedAt,
    Instant createdAt,
    Instant updatedAt
) {}
