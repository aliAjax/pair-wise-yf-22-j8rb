package com.generated.qualityTrace.models;

import java.time.Instant;

public record AuditLog(
    Long id,
    String actor,
    String action,
    String targetType,
    String targetId,
    String message,
    String requestPath,
    String requestMethod,
    Instant createdAt
) {}
