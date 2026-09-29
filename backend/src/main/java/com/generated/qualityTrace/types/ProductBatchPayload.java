package com.generated.qualityTrace.types;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductBatchPayload(
    @NotBlank String batchNo,
    @NotNull Long workOrderId,
    @NotNull @Positive BigDecimal quantity,
    @NotBlank String materialLotNo
) {}
