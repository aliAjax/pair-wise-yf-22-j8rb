package com.generated.qualityTrace.models;

import java.math.BigDecimal;
import java.time.Instant;

public record QualityInspection(
    Long id,
    Long batchId,
    Long inspectorId,
    String inspectionType,
    String standardVersion,
    String resultStatus,
    String previousBatchStatus,
    BigDecimal receivedBatchQuantity,
    Instant inspectedAt,
    Instant createdAt
) {}
