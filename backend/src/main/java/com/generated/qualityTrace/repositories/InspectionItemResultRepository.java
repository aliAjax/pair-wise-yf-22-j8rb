package com.generated.qualityTrace.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.generated.qualityTrace.models.InspectionItemResult;

@Repository
public class InspectionItemResultRepository {
  private final JdbcTemplate jdbc;

  public InspectionItemResultRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long insert(InspectionItemResult item) {
    return jdbc.queryForObject("""
        INSERT INTO inspection_item_result
          (inspection_id, item_code, item_name, measured_value, limit_min, limit_max, item_status)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class,
        item.inspectionId(),
        item.itemCode(),
        item.itemName(),
        item.measuredValue(),
        item.limitMin(),
        item.limitMax(),
        item.itemStatus()
    );
  }

  public List<InspectionItemResult> findByInspectionId(Long inspectionId) {
    return jdbc.query("""
        SELECT * FROM inspection_item_result
        WHERE inspection_id = ?
        ORDER BY id ASC
        """, new InspectionItemResultRowMapper(), inspectionId);
  }

  public List<InspectionItemResult> findByBatchInspectionIds(List<Long> inspectionIds) {
    if (inspectionIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", inspectionIds.stream().map(id -> "?").toList());
    return jdbc.query("""
        SELECT * FROM inspection_item_result
        WHERE inspection_id IN (%s)
        ORDER BY inspection_id ASC, id ASC
        """.formatted(placeholders), new InspectionItemResultRowMapper(), inspectionIds.toArray());
  }

  private static final class InspectionItemResultRowMapper implements RowMapper<InspectionItemResult> {
    @Override
    public InspectionItemResult mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new InspectionItemResult(
          rs.getLong("id"),
          rs.getLong("inspection_id"),
          rs.getString("item_code"),
          rs.getString("item_name"),
          rs.getBigDecimal("measured_value"),
          rs.getBigDecimal("limit_min"),
          rs.getBigDecimal("limit_max"),
          rs.getString("item_status"),
          rs.getObject("created_at", Instant.class)
      );
    }
  }
}
