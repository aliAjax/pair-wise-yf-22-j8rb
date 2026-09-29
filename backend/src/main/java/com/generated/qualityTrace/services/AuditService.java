package com.generated.qualityTrace.services;

import com.generated.qualityTrace.models.AuditLog;
import com.generated.qualityTrace.repositories.AuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  public Long record(String actor, String action, String targetType, String targetId,
                     String message, String path, String method) {
    return auditLogRepository.insert(new AuditLog(
        null,
        actor,
        action,
        targetType,
        targetId,
        message,
        path,
        method,
        Instant.now()
    ));
  }
}
