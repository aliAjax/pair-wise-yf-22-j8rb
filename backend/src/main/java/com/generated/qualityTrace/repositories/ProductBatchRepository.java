package com.generated.qualityTrace.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.generated.qualityTrace.models.ProductBatch;

@Repository
public class ProductBatchRepository {
  private final JdbcTemplate jdbc;

  public ProductBatchRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long insert(ProductBatch batch) {
    return jdbc.queryForObject("""
        INSERT INTO product_batch
          (batch_no, work_order_id, quantity, material_lot_no, produced_at, batch_status)
        VALUES (?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class,
        batch.batchNo(),
        batch.workOrderId(),
        batch.quantity(),
        batch.materialLotNo(),
        batch.producedAt(),
        batch.batchStatus()
    );
  }

  public Optional<ProductBatch> findById(Long id) {
    return queryFirst("SELECT * FROM product_batch WHERE id = ?", id);
  }

  public Optional<ProductBatch> findByBatchNo(String batchNo) {
    return queryFirst("SELECT * FROM product_batch WHERE batch_no = ?", batchNo);
  }

  public List<ProductBatch> findByWorkOrderId(Long workOrderId) {
    return jdbc.query("SELECT * FROM product_batch WHERE work_order_id = ? ORDER BY id",
        new ProductBatchRowMapper(), workOrderId);
  }

  public List<ProductBatch> findAll() {
    return jdbc.query("SELECT * FROM product_batch ORDER BY id DESC", new ProductBatchRowMapper());
  }

  public ProductBatch lockById(Long id) {
    return jdbc.query("SELECT * FROM product_batch WHERE id = ? FOR UPDATE",
        new ProductBatchRowMapper(), id).stream().findFirst().orElse(null);
  }

  private Optional<ProductBatch> queryFirst(String sql, Object... args) {
    return jdbc.query(sql, new ProductBatchRowMapper(), args).stream().findFirst();
  }

  private static final class ProductBatchRowMapper implements RowMapper<ProductBatch> {
    @Override
    public ProductBatch mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ProductBatch(
          rs.getLong("id"),
          rs.getString("batch_no"),
          rs.getLong("work_order_id"),
          rs.getBigDecimal("quantity"),
          rs.getString("material_lot_no"),
          rs.getObject("produced_at", Instant.class),
          rs.getString("batch_status"),
          rs.getObject("created_at", Instant.class),
          rs.getObject("updated_at", Instant.class)
      );
    }
  }
}
