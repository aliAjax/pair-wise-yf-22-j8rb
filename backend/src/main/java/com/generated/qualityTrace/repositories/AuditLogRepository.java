package com.generated.qualityTrace.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.generated.qualityTrace.models.AuditLog;

@Repository
public class AuditLogRepository {
  private final JdbcTemplate jdbc;

  public AuditLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long insert(AuditLog log) {
    return jdbc.queryForObject("""
        INSERT INTO audit_log
          (actor, action, target_type, target_id, message, request_path, request_method, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class,
        log.actor(),
        log.action(),
        log.targetType(),
        log.targetId(),
        log.message(),
        log.requestPath(),
        log.requestMethod(),
        log.createdAt()
    );
  }

  public List<AuditLog> findByTarget(String targetType, String targetId) {
    return jdbc.query("""
        SELECT * FROM audit_log
        WHERE target_type = ? AND target_id = ?
        ORDER BY created_at ASC, id ASC
        """, new AuditLogRowMapper(), targetType, targetId);
  }

  public List<AuditLog> findByTargetIds(String targetType, List<String> targetIds) {
    if (targetIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", targetIds.stream().map(id -> "?").toList());
    Object[] args = new Object[targetIds.size() + 1];
    args[0] = targetType;
    for (int i = 0; i < targetIds.size(); i++) {
      args[i + 1] = targetIds.get(i);
    }
    return jdbc.query("""
        SELECT * FROM audit_log
        WHERE target_type = ? AND target_id IN (%s)
        ORDER BY created_at ASC, id ASC
        """.formatted(placeholders), new AuditLogRowMapper(), args);
  }

  private static final class AuditLogRowMapper implements RowMapper<AuditLog> {
    @Override
    public AuditLog mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new AuditLog(
          rs.getLong("id"),
          rs.getString("actor"),
          rs.getString("action"),
          rs.getString("target_type"),
          rs.getString("target_id"),
          rs.getString("message"),
          rs.getString("request_path"),
          rs.getString("request_method"),
          rs.getObject("created_at", Instant.class)
      );
    }
  }
}
