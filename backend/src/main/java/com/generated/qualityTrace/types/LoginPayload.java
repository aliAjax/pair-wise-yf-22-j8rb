package com.generated.qualityTrace.types;

import jakarta.validation.constraints.NotBlank;

public record LoginPayload(
    @NotBlank String username,
    @NotBlank String password
) {}
