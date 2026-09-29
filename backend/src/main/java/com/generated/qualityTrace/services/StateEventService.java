package com.generated.qualityTrace.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.models.StateEvent;
import com.generated.qualityTrace.repositories.StateEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 状态事件服务（事件溯源）。
 *
 * 规则：
 *  - 只追加 append，不提供 update/delete；
 *  - 当前状态由最新事件的 toState 推出（currentState），绝不直接覆盖旧值；
 *  - 同一聚合携带相同幂等键时，重放既有事件而不是再写一条。
 */
@Service
public class StateEventService {

    private static final Logger log = LoggerFactory.getLogger(StateEventService.class);

    private final StateEventRepository stateEventRepository;
    private final ObjectMapper objectMapper;

    public StateEventService(StateEventRepository stateEventRepository, ObjectMapper objectMapper) {
        this.stateEventRepository = stateEventRepository;
        this.objectMapper = objectMapper;
    }

    /** 追加一条状态事件。 */
    public StateEvent append(String aggregateType, Long aggregateId, String eventType,
                             String fromState, String toState, Map<String, Object> summary) {
        return append(aggregateType, aggregateId, eventType, fromState, toState, summary, null);
    }

    /**
     * 追加一条状态事件（带幂等键）。
     * 幂等键冲突时返回已存在的事件——并发重复回传由数据库唯一索引兜底。
     */
    public StateEvent append(String aggregateType, Long aggregateId, String eventType,
                             String fromState, String toState, Map<String, Object> summary,
                             String idempotencyKey) {
        StateEvent event = new StateEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setFromState(fromState);
        event.setToState(toState);
        event.setChangeSummary(toJson(summary));
        event.setActor(CurrentUser.username());
        event.setIdempotencyKey(idempotencyKey);
        try {
            stateEventRepository.insert(event);
            return event;
        } catch (DuplicateKeyException dup) {
            // 正常路径下批次行锁 + 前置 findByIdempotencyKey 已把重复回传挡在插入之前。
            // 走到这里说明绕过了业务层（如直接写库），PG 事务已 aborted，不能再查询，
            // 直接回滚当前事务；首次提交的那条事件与数据完好无损。
            log.warn("state_event 唯一索引冲突，回滚当前事务 type={} id={} key={}",
                    aggregateType, aggregateId, idempotencyKey);
            throw new com.generated.qualityTrace.exceptions.ConflictException(
                    com.generated.qualityTrace.constants.ErrorCodes.IDEMPOTENCY_MISMATCH,
                    "幂等键 " + idempotencyKey + " 已存在且当前请求无法回放，请先重放或更换键");
        }
    }

    /** 重放完整事件流。 */
    public List<StateEvent> stream(String aggregateType, Long aggregateId) {
        return stateEventRepository.findStream(aggregateType, aggregateId);
    }

    /** 按幂等键找已写入事件（重复回传时回放）。 */
    public java.util.Optional<StateEvent> findByIdempotencyKey(String aggregateType,
                                                               Long aggregateId,
                                                               String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return java.util.Optional.empty();
        }
        return stateEventRepository.findByIdempotencyKey(
                aggregateType, aggregateId, idempotencyKey);
    }

    /** 当前状态：事件流最后一条的 toState；无事件返回 null。 */
    public String currentState(String aggregateType, Long aggregateId) {
        List<StateEvent> events = stateEventRepository.findStream(aggregateType, aggregateId);
        return events.isEmpty() ? null : events.get(events.size() - 1).getToState();
    }

    /** 追溯视图：把事件转成有序 Map。 */
    public List<Map<String, Object>> streamView(String aggregateType, Long aggregateId) {
        return stream(aggregateType, aggregateId).stream()
                .map(this::eventView)
                .toList();
    }

    public Map<String, Object> eventView(StateEvent event) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("seq", event.getId());
        view.put("eventType", event.getEventType());
        view.put("fromState", event.getFromState());
        view.put("toState", event.getToState());
        view.put("change", parseJson(event.getChangeSummary()));
        view.put("actor", event.getActor());
        view.put("occurredAt", event.getOccurredAt());
        return view;
    }

    private String toJson(Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("状态事件摘要序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of("_raw", json);
        }
    }
}
