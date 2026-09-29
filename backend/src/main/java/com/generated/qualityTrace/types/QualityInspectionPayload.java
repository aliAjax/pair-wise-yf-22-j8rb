package com.generated.qualityTrace.types;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record QualityInspectionPayload(
    @NotBlank String batchNo,
    @NotBlank String inspectionType,
    @NotBlank String standardVersion,
    String resultStatus,
    @Valid @NotEmpty List<@NotNull InspectionItemPayload> items
) {}
