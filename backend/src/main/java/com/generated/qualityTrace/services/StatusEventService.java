package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.EntityType;
import com.generated.qualityTrace.models.StatusEvent;
import com.generated.qualityTrace.repositories.StatusEventRepository;
import com.generated.qualityTrace.utils.JsonUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatusEventService {
  private final StatusEventRepository statusEventRepository;

  public StatusEventService(StatusEventRepository statusEventRepository) {
    this.statusEventRepository = statusEventRepository;
  }

  public Long append(EntityType entityType, Long entityId, String eventType, String fromStatus,
                     String toStatus, Object payload, String actor) {
    return statusEventRepository.append(new StatusEvent(
        null,
        entityType.name(),
        entityId,
        eventType,
        fromStatus,
        toStatus,
        JsonUtils.write(payload == null ? Map.of() : payload),
        actor,
        Instant.now()
    ));
  }

  public List<StatusEvent> findByEntity(EntityType entityType, Long entityId) {
    return statusEventRepository.findByEntity(entityType.name(), entityId);
  }

  public List<StatusEvent> findByEntities(EntityType entityType, List<Long> entityIds) {
    return statusEventRepository.findByEntities(entityType.name(), entityIds);
  }

  public String latestStatus(EntityType entityType, Long entityId, String fallbackStatus) {
    List<StatusEvent> events = findByEntity(entityType, entityId);
    return events.isEmpty() ? fallbackStatus : events.get(events.size() - 1).toStatus();
  }
}
