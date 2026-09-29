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
import com.generated.qualityTrace.models.QualityInspection;

@Repository
public class QualityInspectionRepository {
  private final JdbcTemplate jdbc;

  public QualityInspectionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long insert(QualityInspection inspection) {
    return jdbc.queryForObject("""
        INSERT INTO quality_inspection
          (batch_id, inspector_id, inspection_type, standard_version, result_status,
           previous_batch_status, received_batch_quantity, inspected_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class,
        inspection.batchId(),
        inspection.inspectorId(),
        inspection.inspectionType(),
        inspection.standardVersion(),
        inspection.resultStatus(),
        inspection.previousBatchStatus(),
        inspection.receivedBatchQuantity(),
        inspection.inspectedAt()
    );
  }

  public Optional<QualityInspection> findById(Long id) {
    return jdbc.query("SELECT * FROM quality_inspection WHERE id = ?",
        new QualityInspectionRowMapper(), id).stream().findFirst();
  }

  public List<QualityInspection> findByBatchId(Long batchId) {
    return jdbc.query("""
        SELECT * FROM quality_inspection
        WHERE batch_id = ?
        ORDER BY inspected_at ASC, id ASC
        """, new QualityInspectionRowMapper(), batchId);
  }

  private static final class QualityInspectionRowMapper implements RowMapper<QualityInspection> {
    @Override
    public QualityInspection mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new QualityInspection(
          rs.getLong("id"),
          rs.getLong("batch_id"),
          rs.getLong("inspector_id"),
          rs.getString("inspection_type"),
          rs.getString("standard_version"),
          rs.getString("result_status"),
          rs.getString("previous_batch_status"),
          rs.getBigDecimal("received_batch_quantity"),
          rs.getObject("inspected_at", Instant.class),
          rs.getObject("created_at", Instant.class)
      );
    }
  }
}
