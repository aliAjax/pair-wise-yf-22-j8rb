package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.StatusEvent;
import com.generated.qualityTrace.utils.JsonUtils;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatusEventDtoFactory {
  private StatusEventDtoFactory() {}

  public static Map<String, Object> response(StatusEvent event) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", event.id());
    dto.put("entityType", event.entityType());
    dto.put("entityId", event.entityId());
    dto.put("eventType", event.eventType());
    dto.put("fromStatus", event.fromStatus());
    dto.put("toStatus", event.toStatus());
    dto.put("payload", JsonUtils.readTree(event.eventPayload()));
    dto.put("actor", event.actor());
    dto.put("occurredAt", event.occurredAt());
    return dto;
  }
}
