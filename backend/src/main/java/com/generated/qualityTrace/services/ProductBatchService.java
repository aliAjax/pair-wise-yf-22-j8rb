package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.BatchStatus;
import com.generated.qualityTrace.constants.EntityType;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constants.WorkOrderStatus;
import com.generated.qualityTrace.constructors.ProductBatchDtoFactory;
import com.generated.qualityTrace.constructors.StatusEventDtoFactory;
import com.generated.qualityTrace.exceptions.ConflictException;
import com.generated.qualityTrace.exceptions.IllegalBusinessStateException;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.repositories.WorkOrderRepository;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.ProductBatchPayload;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductBatchService {
  private final ProductBatchRepository productBatchRepository;
  private final WorkOrderRepository workOrderRepository;
  private final StatusEventService statusEventService;
  private final AuditService auditService;

  public ProductBatchService(
      ProductBatchRepository productBatchRepository,
      WorkOrderRepository workOrderRepository,
      StatusEventService statusEventService,
      AuditService auditService
  ) {
    this.productBatchRepository = productBatchRepository;
    this.workOrderRepository = workOrderRepository;
    this.statusEventService = statusEventService;
    this.auditService = auditService;
  }

  @Transactional
  public Map<String, Object> create(ProductBatchPayload payload, ActorContext actor, String path) {
    productBatchRepository.findByBatchNo(payload.batchNo()).ifPresent(existing -> {
      throw new ConflictException(ErrorMessages.DUPLICATE_BATCH.formatted(payload.batchNo()));
    });

    WorkOrder workOrder = workOrderRepository.lockById(payload.workOrderId());
    if (workOrder == null) {
      throw new NotFoundException(
          ErrorMessages.WORK_ORDER_NOT_FOUND.formatted(payload.workOrderId()));
    }
    String currentWorkOrderStatus = currentWorkOrderStatus(workOrder);
    if (!WorkOrderStatus.RUNNING.name().equals(currentWorkOrderStatus)) {
      throw new IllegalBusinessStateException(ErrorMessages.WORK_ORDER_NOT_RUNNING);
    }

    BigDecimal existingQty = workOrderRepository.sumBatchQuantity(workOrder.id());
    BigDecimal newTotal = existingQty.add(payload.quantity());
    if (newTotal.compareTo(workOrder.plannedQty()) > 0) {
      throw new IllegalBusinessStateException(
          ErrorMessages.PLANNED_QTY_EXCEEDED.formatted(newTotal, workOrder.plannedQty()));
    }

    Instant now = Instant.now();
    Long id = productBatchRepository.insert(new ProductBatch(
        null,
        payload.batchNo(),
        workOrder.id(),
        payload.quantity(),
        payload.materialLotNo(),
        now,
        BatchStatus.MATERIAL_ISSUED.name(),
        now,
        now
    ));

    Map<String, Object> eventPayload = Map.of(
        "batchNo", payload.batchNo(),
        "quantity", payload.quantity(),
        "materialLotNo", payload.materialLotNo(),
        "workOrderNo", workOrder.orderNo()
    );
    statusEventService.append(EntityType.BATCH, id, "MATERIAL_ISSUED", null,
        BatchStatus.MATERIAL_ISSUED.name(), eventPayload, actor.username());
    auditService.record(actor.username(), "MATERIAL_ISSUE_BATCH", "BATCH", id.toString(),
        LogTemplates.BATCH_CREATED.formatted(
            payload.batchNo(), workOrder.orderNo(), payload.quantity(), payload.materialLotNo()),
        path, "POST");

    return get(id);
  }

  public List<Map<String, Object>> list() {
    return productBatchRepository.findAll().stream()
        .map(batch -> ProductBatchDtoFactory.response(batch, currentBatchStatus(batch)))
        .toList();
  }

  public Map<String, Object> get(Long id) {
    ProductBatch batch = getBatch(id);
    return ProductBatchDtoFactory.response(batch, currentBatchStatus(batch));
  }

  public Map<String, Object> getByBatchNo(String batchNo) {
    ProductBatch batch = getBatch(batchNo);
    return ProductBatchDtoFactory.response(batch, currentBatchStatus(batch));
  }

  public List<Map<String, Object>> events(String batchNo) {
    ProductBatch batch = getBatch(batchNo);
    return statusEventService.findByEntity(EntityType.BATCH, batch.id()).stream()
        .map(StatusEventDtoFactory::response)
        .toList();
  }

  ProductBatch getBatch(Long id) {
    return productBatchRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorMessages.BATCH_NOT_FOUND.formatted(id)));
  }

  ProductBatch getBatch(String batchNo) {
    return productBatchRepository.findByBatchNo(batchNo)
        .orElseThrow(() -> new NotFoundException(ErrorMessages.BATCH_NOT_FOUND.formatted(batchNo)));
  }

  String currentBatchStatus(ProductBatch batch) {
    return statusEventService.latestStatus(EntityType.BATCH, batch.id(), batch.batchStatus());
  }

  private String currentWorkOrderStatus(WorkOrder workOrder) {
    return statusEventService.latestStatus(
        EntityType.WORK_ORDER, workOrder.id(), workOrder.status());
  }
}
