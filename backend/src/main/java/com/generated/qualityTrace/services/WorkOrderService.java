package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.EntityType;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constants.WorkOrderStatus;
import com.generated.qualityTrace.constructors.StatusEventDtoFactory;
import com.generated.qualityTrace.constructors.WorkOrderDtoFactory;
import com.generated.qualityTrace.exceptions.ConflictException;
import com.generated.qualityTrace.exceptions.IllegalBusinessStateException;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.models.StatusEvent;
import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.repositories.WorkOrderRepository;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.WorkOrderActionPayload;
import com.generated.qualityTrace.types.WorkOrderPayload;
import com.generated.qualityTrace.validators.ProductCodeValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {
  private final WorkOrderRepository workOrderRepository;
  private final StatusEventService statusEventService;
  private final AuditService auditService;
  private final ProductCodeValidator productCodeValidator;

  public WorkOrderService(
      WorkOrderRepository workOrderRepository,
      StatusEventService statusEventService,
      AuditService auditService,
      ProductCodeValidator productCodeValidator
  ) {
    this.workOrderRepository = workOrderRepository;
    this.statusEventService = statusEventService;
    this.auditService = auditService;
    this.productCodeValidator = productCodeValidator;
  }

  @Transactional
  public Map<String, Object> create(WorkOrderPayload payload, ActorContext actor, String path) {
    productCodeValidator.validate(payload.productCode());
    workOrderRepository.findByOrderNo(payload.orderNo()).ifPresent(existing -> {
      throw new ConflictException(ErrorMessages.DUPLICATE_WORK_ORDER.formatted(payload.orderNo()));
    });

    Long id = workOrderRepository.insert(new WorkOrder(
        null,
        payload.orderNo(),
        payload.productCode(),
        payload.productName(),
        payload.plannedQty(),
        payload.lineCode(),
        null,
        WorkOrderStatus.PLANNED.name(),
        null,
        null
    ));

    statusEventService.append(EntityType.WORK_ORDER, id, "WORK_ORDER_CREATED", null,
        WorkOrderStatus.PLANNED.name(), Map.of(
            "orderNo", payload.orderNo(),
            "productCode", payload.productCode(),
            "plannedQty", payload.plannedQty(),
            "lineCode", payload.lineCode()
        ), actor.username());
    auditService.record(actor.username(), "CREATE_WORK_ORDER", "WORK_ORDER", id.toString(),
        LogTemplates.WORK_ORDER_CREATED.formatted(
            payload.orderNo(), payload.productCode(), payload.plannedQty()),
        path, "POST");

    return get(id);
  }

  public List<Map<String, Object>> list() {
    return workOrderRepository.findAll().stream().map(workOrder -> {
      String currentStatus = currentStatus(workOrder);
      BigDecimal completedQty = workOrderRepository.sumBatchQuantity(workOrder.id());
      return WorkOrderDtoFactory.response(
          workOrder, currentStatus, completedQty, workOrder.plannedQty().subtract(completedQty));
    }).toList();
  }

  public Map<String, Object> get(Long id) {
    return toResponse(getWorkOrder(id));
  }

  public Map<String, Object> getByOrderNo(String orderNo) {
    WorkOrder workOrder = workOrderRepository.findByOrderNo(orderNo)
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.WORK_ORDER_NOT_FOUND.formatted(orderNo)));
    return toResponse(workOrder);
  }

  @Transactional
  public Map<String, Object> changeStatus(Long id, WorkOrderStatus target,
                                          WorkOrderActionPayload payload, ActorContext actor,
                                          String path, String method) {
    WorkOrder workOrder = workOrderRepository.lockById(id);
    if (workOrder == null) {
      throw new NotFoundException(ErrorMessages.WORK_ORDER_NOT_FOUND.formatted(id));
    }

    String fromStatusName = currentStatus(workOrder);
    WorkOrderStatus fromStatus = WorkOrderStatus.valueOf(fromStatusName);
    if (!fromStatus.canTransitionTo(target)) {
      throw new IllegalBusinessStateException(
          ErrorMessages.ILLEGAL_TRANSITION.formatted(fromStatus, target));
    }

    if (target == WorkOrderStatus.FINISHED) {
      BigDecimal completedQty = workOrderRepository.sumBatchQuantity(id);
      if (completedQty.compareTo(workOrder.plannedQty()) < 0) {
        throw new IllegalBusinessStateException(
            ErrorMessages.FINISH_QTY_NOT_ENOUGH.formatted(completedQty, workOrder.plannedQty()));
      }
    }

    Instant startAt = workOrder.startAt();
    if (target == WorkOrderStatus.RUNNING && startAt == null) {
      startAt = Instant.now();
    }
    workOrderRepository.updateStartAt(id, startAt);

    String eventType = "WORK_ORDER_" + target.name();
    statusEventService.append(EntityType.WORK_ORDER, id, eventType, fromStatus.name(),
        target.name(), Map.of("note", payload == null ? "" : payload.note()), actor.username());
    auditService.record(actor.username(), eventType, "WORK_ORDER", id.toString(),
        LogTemplates.WORK_ORDER_STATUS_CHANGED.formatted(
            workOrder.orderNo(), fromStatus.name(), target.name()),
        path, method);

    return get(id);
  }

  public List<Map<String, Object>> events(Long id) {
    getWorkOrder(id);
    return statusEventService.findByEntity(EntityType.WORK_ORDER, id).stream()
        .map(StatusEventDtoFactory::response)
        .toList();
  }

  private Map<String, Object> toResponse(WorkOrder workOrder) {
    BigDecimal completedQty = workOrderRepository.sumBatchQuantity(workOrder.id());
    return WorkOrderDtoFactory.response(
        workOrder,
        currentStatus(workOrder),
        completedQty,
        workOrder.plannedQty().subtract(completedQty)
    );
  }

  WorkOrder getWorkOrder(Long id) {
    return workOrderRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.WORK_ORDER_NOT_FOUND.formatted(id)));
  }

  String currentStatus(WorkOrder workOrder) {
    List<StatusEvent> events = statusEventService.findByEntity(EntityType.WORK_ORDER, workOrder.id());
    return events.isEmpty() ? workOrder.status() : events.get(events.size() - 1).toStatus();
  }
}
