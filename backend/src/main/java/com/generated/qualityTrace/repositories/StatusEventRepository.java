package com.generated.qualityTrace.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.generated.qualityTrace.models.StatusEvent;

@Repository
public class StatusEventRepository {
  private final JdbcTemplate jdbc;

  public StatusEventRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long append(StatusEvent event) {
    return jdbc.queryForObject("""
        INSERT INTO status_event
          (entity_type, entity_id, event_type, from_status, to_status, event_payload, actor, occurred_at)
        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
        RETURNING id
        """, Long.class,
        event.entityType(),
        event.entityId(),
        event.eventType(),
        event.fromStatus(),
        event.toStatus(),
        event.eventPayload(),
        event.actor(),
        event.occurredAt()
    );
  }

  public List<StatusEvent> findByEntity(String entityType, Long entityId) {
    return jdbc.query("""
        SELECT * FROM status_event
        WHERE entity_type = ? AND entity_id = ?
        ORDER BY occurred_at ASC, id ASC
        """, new StatusEventRowMapper(), entityType, entityId);
  }

  public List<StatusEvent> findByEntities(String entityType, List<Long> entityIds) {
    if (entityIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", entityIds.stream().map(id -> "?").toList());
    Object[] args = new Object[entityIds.size() + 1];
    args[0] = entityType;
    for (int i = 0; i < entityIds.size(); i++) {
      args[i + 1] = entityIds.get(i);
    }
    return jdbc.query("""
        SELECT * FROM status_event
        WHERE entity_type = ? AND entity_id IN (%s)
        ORDER BY entity_id ASC, occurred_at ASC, id ASC
        """.formatted(placeholders), new StatusEventRowMapper(), args);
  }

  private static final class StatusEventRowMapper implements RowMapper<StatusEvent> {
    @Override
    public StatusEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new StatusEvent(
          rs.getLong("id"),
          rs.getString("entity_type"),
          rs.getLong("entity_id"),
          rs.getString("event_type"),
          rs.getString("from_status"),
          rs.getString("to_status"),
          rs.getString("event_payload"),
          rs.getString("actor"),
          rs.getObject("occurred_at", Instant.class)
      );
    }
  }
}
