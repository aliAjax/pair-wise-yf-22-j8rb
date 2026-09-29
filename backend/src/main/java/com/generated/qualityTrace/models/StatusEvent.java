package com.generated.qualityTrace.models;

import java.time.Instant;

public record StatusEvent(
    Long id,
    String entityType,
    Long entityId,
    String eventType,
    String fromStatus,
    String toStatus,
    String eventPayload,
    String actor,
    Instant occurredAt
) {}
