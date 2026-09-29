package com.generated.qualityTrace.types;

import jakarta.validation.constraints.NotBlank;

public record CloseDefectPayload(
    @NotBlank String note
) {}
