package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.generated.qualityTrace.repositories.PostgresJsonbTypeHandler;

import java.time.OffsetDateTime;

/**
 * 状态事件（事件溯源）。只追加，不更新、不删除。
 * 当前状态 = 该聚合下 id 最大事件的 toState。
 */
@TableName(value = "state_event", autoResultMap = true)
public class StateEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String aggregateType;   // WORK_ORDER / BATCH / DEFECT
    private Long aggregateId;
    private String eventType;
    private String fromState;
    private String toState;

    @TableField(typeHandler = PostgresJsonbTypeHandler.class)
    private String changeSummary;   // JSONB 文本：变更前后的数量/判定等
    private String actor;
    private String idempotencyKey;
    private OffsetDateTime occurredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getFromState() { return fromState; }
    public void setFromState(String fromState) { this.fromState = fromState; }
    public String getToState() { return toState; }
    public void setToState(String toState) { this.toState = toState; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}
