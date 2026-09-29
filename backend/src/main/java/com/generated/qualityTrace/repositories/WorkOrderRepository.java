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
import com.generated.qualityTrace.models.WorkOrder;

@Repository
public class WorkOrderRepository {
  private final JdbcTemplate jdbc;

  public WorkOrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long insert(WorkOrder workOrder) {
    return jdbc.queryForObject("""
        INSERT INTO work_order
          (order_no, product_code, product_name, planned_qty, line_code, start_at, status)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class,
        workOrder.orderNo(),
        workOrder.productCode(),
        workOrder.productName(),
        workOrder.plannedQty(),
        workOrder.lineCode(),
        workOrder.startAt(),
        workOrder.status()
    );
  }

  public Optional<WorkOrder> findById(Long id) {
    return queryFirst("SELECT * FROM work_order WHERE id = ?", id);
  }

  public Optional<WorkOrder> findByOrderNo(String orderNo) {
    return queryFirst("SELECT * FROM work_order WHERE order_no = ?", orderNo);
  }

  public List<WorkOrder> findAll() {
    return jdbc.query("SELECT * FROM work_order ORDER BY id DESC", new WorkOrderRowMapper());
  }

  public WorkOrder lockById(Long id) {
    return jdbc.query("SELECT * FROM work_order WHERE id = ? FOR UPDATE",
        new WorkOrderRowMapper(), id).stream().findFirst().orElse(null);
  }

  public void updateStartAt(Long id, Instant startAt) {
    jdbc.update("UPDATE work_order SET start_at = ?, updated_at = now() WHERE id = ?",
        startAt, id);
  }

  public BigDecimal sumBatchQuantity(Long workOrderId) {
    BigDecimal value = jdbc.queryForObject(
        "SELECT COALESCE(SUM(quantity), 0) FROM product_batch WHERE work_order_id = ?",
        BigDecimal.class, workOrderId);
    return value == null ? BigDecimal.ZERO : value;
  }

  private Optional<WorkOrder> queryFirst(String sql, Object... args) {
    return jdbc.query(sql, new WorkOrderRowMapper(), args).stream().findFirst();
  }

  private static final class WorkOrderRowMapper implements RowMapper<WorkOrder> {
    @Override
    public WorkOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new WorkOrder(
          rs.getLong("id"),
          rs.getString("order_no"),
          rs.getString("product_code"),
          rs.getString("product_name"),
          rs.getBigDecimal("planned_qty"),
          rs.getString("line_code"),
          rs.getObject("start_at", Instant.class),
          rs.getString("status"),
          rs.getObject("created_at", Instant.class),
          rs.getObject("updated_at", Instant.class)
      );
    }
  }
}
