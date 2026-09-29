package com.generated.qualityTrace.types;

import jakarta.validation.constraints.NotBlank;

public record WorkOrderActionPayload(
    @NotBlank String note
) {}
