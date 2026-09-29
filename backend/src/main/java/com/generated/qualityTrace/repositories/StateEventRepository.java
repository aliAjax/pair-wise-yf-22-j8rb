package com.generated.qualityTrace.repositories;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.generated.qualityTrace.models.StateEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StateEventRepository extends BaseMapper<StateEvent> {

    /** 重放某聚合的完整事件流（按写入顺序）。 */
    @Select("SELECT * FROM state_event "
            + "WHERE aggregate_type = #{aggregateType} AND aggregate_id = #{aggregateId} "
            + "ORDER BY id ASC")
    List<StateEvent> findStream(@Param("aggregateType") String aggregateType,
                                @Param("aggregateId") Long aggregateId);

    /** 幂等回放：按聚合 + 幂等键找已写入事件。 */
    @Select("SELECT * FROM state_event "
            + "WHERE aggregate_type = #{aggregateType} AND aggregate_id = #{aggregateId} "
            + "AND idempotency_key = #{idempotencyKey} LIMIT 1")
    java.util.Optional<StateEvent> findByIdempotencyKey(
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") Long aggregateId,
            @Param("idempotencyKey") String idempotencyKey);
}
