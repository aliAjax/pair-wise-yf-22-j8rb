package com.generated.qualityTrace.types;

import jakarta.validation.constraints.NotBlank;

public record DefectDispositionPayload(
    @NotBlank String dispositionStatus,
    String note
) {}
