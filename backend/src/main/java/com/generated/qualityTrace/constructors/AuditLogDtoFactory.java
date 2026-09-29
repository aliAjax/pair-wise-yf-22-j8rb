package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.AuditLog;
import com.generated.qualityTrace.utils.Formatters;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditLogDtoFactory {
  private AuditLogDtoFactory() {}

  public static Map<String, Object> response(AuditLog log) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", log.id());
    dto.put("actor", log.actor());
    dto.put("action", log.action());
    dto.put("targetType", log.targetType());
    dto.put("targetId", log.targetId());
    dto.put("message", log.message());
    dto.put("path", log.requestPath());
    dto.put("method", log.requestMethod());
    dto.put("createdAt", Formatters.instant(log.createdAt()));
    return dto;
  }
}
