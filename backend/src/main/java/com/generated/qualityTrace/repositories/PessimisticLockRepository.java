package com.generated.qualityTrace.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 悲观行锁与聚合查询：同一批次多条检验并发回传时，
 * 后到的事务在该行上排队，拿到的是“已接住前一批”后的最新状态与数量。
 */
@Repository
public class PessimisticLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public PessimisticLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 锁住批次行直到事务提交/回滚。不存在返回空。 */
    public Optional<Long> lockBatch(Long batchId) {
        return jdbcTemplate.query(
                "SELECT id FROM product_batch WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(),
                batchId);
    }

    /** 锁住工单行（状态流转串行化）。 */
    public Optional<Long> lockWorkOrder(Long workOrderId) {
        return jdbcTemplate.query(
                "SELECT id FROM work_order WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(),
                workOrderId);
    }

    /** 锁住不良记录行（处置流转串行化，关闭后禁止再动）。 */
    public Optional<Long> lockDefect(Long defectId) {
        return jdbcTemplate.query(
                "SELECT id FROM defect_record WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(),
                defectId);
    }
}
