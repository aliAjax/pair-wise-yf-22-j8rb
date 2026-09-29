package com.generated.qualityTrace.models;

import java.time.Instant;

public record User(
    Long id,
    String username,
    String passwordSalt,
    String passwordHash,
    String displayName,
    String role,
    boolean enabled,
    Instant createdAt
) {}
