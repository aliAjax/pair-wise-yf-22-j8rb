package com.generated.qualityTrace.repositories;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.generated.qualityTrace.models.DefectRecord;

@Repository
public class DefectRecordRepository {
  private final JdbcTemplate jdbc;

  public DefectRecordRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long insert(DefectRecord defect) {
    return jdbc.queryForObject("""
        INSERT INTO defect_record
          (batch_id, defect_type, defect_qty, severity, root_cause,
           original_disposition, disposition_status, registered_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class,
        defect.batchId(),
        defect.defectType(),
        defect.defectQty(),
        defect.severity(),
        defect.rootCause(),
        defect.originalDisposition(),
        defect.dispositionStatus(),
        defect.registeredAt()
    );
  }

  public Optional<DefectRecord> findById(Long id) {
    return jdbc.query("SELECT * FROM defect_record WHERE id = ?",
        new DefectRecordRowMapper(), id).stream().findFirst();
  }

  public DefectRecord lockById(Long id) {
    return jdbc.query("SELECT * FROM defect_record WHERE id = ? FOR UPDATE",
        new DefectRecordRowMapper(), id).stream().findFirst().orElse(null);
  }

  public List<DefectRecord> findByBatchId(Long batchId) {
    return jdbc.query("""
        SELECT * FROM defect_record
        WHERE batch_id = ?
        ORDER BY registered_at ASC, id ASC
        """, new DefectRecordRowMapper(), batchId);
  }

  public BigDecimal sumOpenDefectQuantity(Long batchId) {
    BigDecimal value = jdbc.queryForObject("""
        SELECT COALESCE(SUM(d.defect_qty), 0)
        FROM defect_record d
        LEFT JOIN LATERAL (
          SELECT to_status
          FROM status_event e
          WHERE e.entity_type = 'DEFECT' AND e.entity_id = d.id
          ORDER BY e.occurred_at DESC, e.id DESC
          LIMIT 1
        ) latest ON TRUE
        WHERE d.batch_id = ? AND COALESCE(latest.to_status, d.disposition_status) <> 'CLOSED'
        """, BigDecimal.class, batchId);
    return value == null ? BigDecimal.ZERO : value;
  }

  public void markClosed(Long defectId) {
    jdbc.update("""
        UPDATE defect_record
        SET closed_at = now(), updated_at = now()
        WHERE id = ? AND closed_at IS NULL
        """, defectId);
  }

  private static final class DefectRecordRowMapper implements RowMapper<DefectRecord> {
    @Override
    public DefectRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new DefectRecord(
          rs.getLong("id"),
          rs.getLong("batch_id"),
          rs.getString("defect_type"),
          rs.getBigDecimal("defect_qty"),
          rs.getString("severity"),
          rs.getString("root_cause"),
          rs.getString("original_disposition"),
          rs.getString("disposition_status"),
          rs.getObject("registered_at", Instant.class),
          rs.getObject("closed_at", Instant.class),
          rs.getObject("created_at", Instant.class),
          rs.getObject("updated_at", Instant.class)
      );
    }
  }
}
