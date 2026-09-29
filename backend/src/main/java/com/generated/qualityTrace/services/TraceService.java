package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.EntityType;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constructors.DefectRecordDtoFactory;
import com.generated.qualityTrace.constructors.InspectionItemResultDtoFactory;
import com.generated.qualityTrace.constructors.ProductBatchDtoFactory;
import com.generated.qualityTrace.constructors.StatusEventDtoFactory;
import com.generated.qualityTrace.constructors.WorkOrderDtoFactory;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.models.DefectRecord;
import com.generated.qualityTrace.models.InspectionItemResult;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.models.StatusEvent;
import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.repositories.AuditLogRepository;
import com.generated.qualityTrace.repositories.DefectRecordRepository;
import com.generated.qualityTrace.repositories.InspectionItemResultRepository;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.repositories.QualityInspectionRepository;
import com.generated.qualityTrace.repositories.WorkOrderRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TraceService {
  private final ProductBatchRepository productBatchRepository;
  private final WorkOrderRepository workOrderRepository;
  private final QualityInspectionRepository qualityInspectionRepository;
  private final InspectionItemResultRepository inspectionItemResultRepository;
  private final DefectRecordRepository defectRecordRepository;
  private final StatusEventService statusEventService;
  private final AuditLogRepository auditLogRepository;

  public TraceService(
      ProductBatchRepository productBatchRepository,
      WorkOrderRepository workOrderRepository,
      QualityInspectionRepository qualityInspectionRepository,
      InspectionItemResultRepository inspectionItemResultRepository,
      DefectRecordRepository defectRecordRepository,
      StatusEventService statusEventService,
      AuditLogRepository auditLogRepository
  ) {
    this.productBatchRepository = productBatchRepository;
    this.workOrderRepository = workOrderRepository;
    this.qualityInspectionRepository = qualityInspectionRepository;
    this.inspectionItemResultRepository = inspectionItemResultRepository;
    this.defectRecordRepository = defectRecordRepository;
    this.statusEventService = statusEventService;
    this.auditLogRepository = auditLogRepository;
  }

  public Map<String, Object> traceByBatchNo(String batchNo) {
    ProductBatch batch = productBatchRepository.findByBatchNo(batchNo)
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.BATCH_NOT_FOUND.formatted(batchNo)));
    WorkOrder workOrder = workOrderRepository.findById(batch.workOrderId())
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.WORK_ORDER_NOT_FOUND.formatted(batch.workOrderId())));

    String currentBatchStatus = statusEventService.latestStatus(
        EntityType.BATCH, batch.id(), batch.batchStatus());
    String currentWorkOrderStatus = statusEventService.latestStatus(
        EntityType.WORK_ORDER, workOrder.id(), workOrder.status());
    BigDecimal completedQty = workOrderRepository.sumBatchQuantity(workOrder.id());

    List<QualityInspection> inspections = qualityInspectionRepository.findByBatchId(batch.id());
    List<Long> inspectionIds = inspections.stream().map(QualityInspection::id).toList();
    Map<Long, List<Map<String, Object>>> itemsByInspection = inspectionItemResultRepository
        .findByBatchInspectionIds(inspectionIds)
        .stream()
        .collect(Collectors.groupingBy(
            InspectionItemResult::inspectionId,
            Collectors.mapping(InspectionItemResultDtoFactory::response, Collectors.toList())
        ));

    List<DefectRecord> defects = defectRecordRepository.findByBatchId(batch.id());
    List<Long> defectIds = defects.stream().map(DefectRecord::id).toList();
    Map<Long, List<StatusEvent>> defectEvents = statusEventService
        .findByEntities(EntityType.DEFECT, defectIds)
        .stream()
        .collect(Collectors.groupingBy(StatusEvent::entityId));

    List<Map<String, Object>> inspectionDtos = inspections.stream()
        .map(inspection -> {
          Map<String, Object> dto = new LinkedHashMap<>();
          dto.put("id", inspection.id());
          dto.put("inspectorId", inspection.inspectorId());
          dto.put("inspectionType", inspection.inspectionType());
          dto.put("standardVersion", inspection.standardVersion());
          dto.put("resultStatus", inspection.resultStatus());
          dto.put("previousBatchStatus", inspection.previousBatchStatus());
          dto.put("receivedBatchQuantity", inspection.receivedBatchQuantity());
          dto.put("inspectedAt", inspection.inspectedAt());
          dto.put("items", itemsByInspection.getOrDefault(inspection.id(), List.of()));
          return dto;
        })
        .toList();

    List<Map<String, Object>> defectDtos = defects.stream()
        .map(defect -> {
          String currentDefectStatus = defectEvents.getOrDefault(defect.id(), List.of()).isEmpty()
              ? defect.dispositionStatus()
              : defectEvents.get(defect.id()).get(defectEvents.get(defect.id()).size() - 1).toStatus();
          Map<String, Object> dto = new LinkedHashMap<>(
              DefectRecordDtoFactory.response(defect, currentDefectStatus));
          dto.put("events", defectEvents.getOrDefault(defect.id(), List.of()).stream()
              .map(StatusEventDtoFactory::response)
              .toList());
          return dto;
        })
        .toList();

    List<Map<String, Object>> batchEvents = statusEventService
        .findByEntity(EntityType.BATCH, batch.id()).stream()
        .map(StatusEventDtoFactory::response)
        .toList();
    List<Map<String, Object>> workOrderEvents = statusEventService
        .findByEntity(EntityType.WORK_ORDER, workOrder.id()).stream()
        .map(StatusEventDtoFactory::response)
        .toList();
    List<Map<String, Object>> auditLogs = collectAuditLogs(batch, workOrder, inspectionIds, defectIds);

    Map<String, Object> trace = new LinkedHashMap<>();
    trace.put("batchNo", batch.batchNo());
    trace.put("batch", ProductBatchDtoFactory.response(batch, currentBatchStatus));
    trace.put("workOrder", WorkOrderDtoFactory.response(
        workOrder, currentWorkOrderStatus, completedQty,
        workOrder.plannedQty().subtract(completedQty)));
    trace.put("inspections", inspectionDtos);
    trace.put("defects", defectDtos);
    trace.put("timeline", buildTimeline(workOrderEvents, batchEvents, defects, defectEvents));
    trace.put("batchEvents", batchEvents);
    trace.put("workOrderEvents", workOrderEvents);
    trace.put("auditLogs", auditLogs);
    return trace;
  }

  private List<Map<String, Object>> collectAuditLogs(
      ProductBatch batch,
      WorkOrder workOrder,
      List<Long> inspectionIds,
      List<Long> defectIds
  ) {
    List<com.generated.qualityTrace.models.AuditLog> logs = new java.util.ArrayList<>();
    logs.addAll(auditLogRepository.findByTarget("WORK_ORDER", workOrder.id().toString()));
    logs.addAll(auditLogRepository.findByTarget("BATCH", batch.id().toString()));
    logs.addAll(auditLogRepository.findByTargetIds("INSPECTION",
        inspectionIds.stream().map(String::valueOf).toList()));
    logs.addAll(auditLogRepository.findByTargetIds("DEFECT",
        defectIds.stream().map(String::valueOf).toList()));
    logs.sort((left, right) -> left.createdAt().compareTo(right.createdAt()));
    return logs.stream()
        .map(com.generated.qualityTrace.constructors.AuditLogDtoFactory::response)
        .toList();
  }

  private List<Map<String, Object>> buildTimeline(
      List<Map<String, Object>> workOrderEvents,
      List<Map<String, Object>> batchEvents,
      List<DefectRecord> defects,
      Map<Long, List<StatusEvent>> defectEvents
  ) {
    List<Map<String, Object>> timeline = new java.util.ArrayList<>();
    workOrderEvents.forEach(event -> {
      Map<String, Object> scoped = new LinkedHashMap<>(event);
      scoped.put("scope", "WORK_ORDER");
      timeline.add(scoped);
    });
    batchEvents.forEach(event -> {
      Map<String, Object> scoped = new LinkedHashMap<>(event);
      scoped.put("scope", "BATCH");
      timeline.add(scoped);
    });
    defectEvents.values().stream()
        .flatMap(List::stream)
        .map(StatusEventDtoFactory::response)
        .map(event -> {
          event.put("scope", "DEFECT");
          return event;
        })
        .forEach(timeline::add);
    timeline.sort((left, right) -> ((Comparable<Object>) left.get("occurredAt"))
        .compareTo(right.get("occurredAt")));
    return timeline;
  }
}
