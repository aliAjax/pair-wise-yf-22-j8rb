package com.generated.qualityTrace.services;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.constants.EventTypes;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constants.WorkOrderStatus;
import com.generated.qualityTrace.constructors.WorkOrderDtoFactory;
import com.generated.qualityTrace.exceptions.ConflictException;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.exceptions.ValidationDomainException;
import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.repositories.PessimisticLockRepository;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.repositories.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
    private static final String AGGREGATE = "WORK_ORDER";

    private final WorkOrderRepository workOrderRepository;
    private final ProductBatchRepository productBatchRepository;
    private final PessimisticLockRepository lockRepository;
    private final StateEventService stateEventService;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            ProductBatchRepository productBatchRepository,
                            PessimisticLockRepository lockRepository,
                            StateEventService stateEventService) {
        this.workOrderRepository = workOrderRepository;
        this.productBatchRepository = productBatchRepository;
        this.lockRepository = lockRepository;
        this.stateEventService = stateEventService;
    }

    /** 创建工单 + 首条状态事件 PLANNED。 */
    @Transactional
    public Map<String, Object> create(String orderNo, String productCode, String productName,
                                      Integer plannedQty, String lineCode) {
        Long existing = workOrderRepository.selectCount(
                new QueryWrapper<WorkOrder>().eq("order_no", orderNo));
        if (existing != null && existing > 0) {
            throw new ConflictException("WORK_ORDER_CONFLICT", "工单号已存在：" + orderNo);
        }
        WorkOrder order = new WorkOrder();
        order.setOrderNo(orderNo);
        order.setProductCode(productCode);
        order.setProductName(productName);
        order.setPlannedQty(plannedQty);
        order.setCompletedQty(0);
        order.setLineCode(lineCode);
        workOrderRepository.insert(order);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderNo", orderNo);
        summary.put("productCode", productCode);
        summary.put("plannedQty", plannedQty);
        stateEventService.append(AGGREGATE, order.getId(), EventTypes.ORDER_CREATED,
                null, WorkOrderStatus.PLANNED.name(), summary);

        log.info(LogTemplates.WO_CREATE, orderNo, productCode, plannedQty, CurrentUser.username());
        return detail(order.getId());
    }

    /** 开工：PLANNED/PAUSED -> RUNNING，写 start_at（仅首次）。 */
    @Transactional
    public Map<String, Object> start(Long id) {
        return transit(id, EventTypes.ORDER_STARTED, WorkOrderStatus.ALLOW_START,
                WorkOrderStatus.RUNNING, () -> {
                    WorkOrder order = workOrderRepository.selectById(id);
                    if (order.getStartAt() == null) {
                        order.setStartAt(OffsetDateTime.now());
                        workOrderRepository.updateById(order);
                    }
                });
    }

    @Transactional
    public Map<String, Object> pause(Long id) {
        return transit(id, EventTypes.ORDER_PAUSED, WorkOrderStatus.ALLOW_PAUSE,
                WorkOrderStatus.PAUSED, null);
    }

    @Transactional
    public Map<String, Object> finish(Long id) {
        return transit(id, EventTypes.ORDER_FINISHED, WorkOrderStatus.ALLOW_FINISH,
                WorkOrderStatus.FINISHED, null);
    }

    @Transactional
    public Map<String, Object> cancel(Long id) {
        return transit(id, EventTypes.ORDER_CANCELLED, WorkOrderStatus.ALLOW_CANCEL,
                WorkOrderStatus.CANCELLED, null);
    }

    private Map<String, Object> transit(Long id, String eventType,
                                        java.util.Set<WorkOrderStatus> allowed,
                                        WorkOrderStatus target, Runnable sideEffect) {
        WorkOrder order = requireOrder(id);
        lockRepository.lockWorkOrder(id); // 状态流转串行化
        String from = stateEventService.currentState(AGGREGATE, id);
        WorkOrderStatus fromStatus = from == null ? null : WorkOrderStatus.valueOf(from);
        if (fromStatus == null || !allowed.contains(fromStatus)) {
            throw new ConflictException("WORK_ORDER_CONFLICT",
                    String.format("工单 %s 当前状态 %s，不允许执行 %s",
                            order.getOrderNo(), from, eventType));
        }
        if (sideEffect != null) {
            sideEffect.run();
        }
        stateEventService.append(AGGREGATE, id, eventType, from, target.name(),
                Map.of("lineCode", order.getLineCode() == null ? "" : order.getLineCode()));
        log.info("[WORK_ORDER] 状态流转 orderNo={} {} -> {} event={} actor={}",
                order.getOrderNo(), from, target.name(), eventType, CurrentUser.username());
        return detail(id);
    }

    /** 批次投料后回写工单完工数量（只增不减，接续而非覆盖）。 */
    @Transactional
    public void addProgress(Long workOrderId, int addedQty) {
        lockRepository.lockWorkOrder(workOrderId); // 并发投料串行，避免丢失回写
        WorkOrder order = workOrderRepository.selectById(workOrderId);
        if (order == null) {
            throw NotFoundException.workOrder(String.valueOf(workOrderId));
        }
        int before = order.getCompletedQty() == null ? 0 : order.getCompletedQty();
        int after = before + addedQty;
        if (after > order.getPlannedQty()) {
            throw new ValidationDomainException(String.format(
                    "工单 %s 累计完工 %s 超过计划数量 %s", order.getOrderNo(), after,
                    order.getPlannedQty()));
        }
        order.setCompletedQty(after);
        workOrderRepository.updateById(order);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("completedQtyBefore", before);
        summary.put("completedQtyAfter", after);
        summary.put("addedQty", addedQty);
        stateEventService.append(AGGREGATE, workOrderId, EventTypes.ORDER_PROGRESS_ADDED,
                stateEventService.currentState(AGGREGATE, workOrderId),
                stateEventService.currentState(AGGREGATE, workOrderId), summary);
        log.info(LogTemplates.WO_PROGRESS, order.getOrderNo(), before, after,
                CurrentUser.username());
    }

    public Map<String, Object> detail(Long id) {
        WorkOrder order = requireOrder(id);
        String status = stateEventService.currentState(AGGREGATE, id);
        List<Map<String, Object>> events = stateEventService.streamView(AGGREGATE, id);
        Map<String, Object> dto = WorkOrderDtoFactory.detail(order, status, events);
        long batchCount = productBatchRepository.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<
                        com.generated.qualityTrace.models.ProductBatch>()
                        .eq("work_order_id", id));
        dto.put("hasBatch", batchCount > 0);
        return dto;
    }

    /** 旧工单（无批次）照常列出，不强制回填批次号。 */
    public List<Map<String, Object>> list() {
        return workOrderRepository.selectList(
                        new QueryWrapper<WorkOrder>().orderByDesc("id")).stream()
                .map(o -> WorkOrderDtoFactory.listItem(o,
                        stateEventService.currentState(AGGREGATE, o.getId())))
                .toList();
    }

    private WorkOrder requireOrder(Long id) {
        WorkOrder order = workOrderRepository.selectById(id);
        if (order == null) {
            throw NotFoundException.workOrder(String.valueOf(id));
        }
        return order;
    }
}
