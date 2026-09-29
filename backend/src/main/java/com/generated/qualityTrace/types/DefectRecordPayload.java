package com.generated.qualityTrace.types;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DefectRecordPayload(
    @NotBlank String batchNo,
    @NotBlank String defectType,
    @NotNull @Positive BigDecimal defectQty,
    @NotBlank String severity,
    @NotBlank String rootCause
) {}
