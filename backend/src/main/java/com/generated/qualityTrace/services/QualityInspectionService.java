package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.BatchStatus;
import com.generated.qualityTrace.constants.EntityType;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.InspectionResultStatus;
import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constructors.InspectionItemResultDtoFactory;
import com.generated.qualityTrace.constructors.QualityInspectionDtoFactory;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.models.InspectionItemResult;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.repositories.InspectionItemResultRepository;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.repositories.QualityInspectionRepository;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.InspectionItemPayload;
import com.generated.qualityTrace.types.QualityInspectionPayload;
import com.generated.qualityTrace.validators.EnumValidator;
import com.generated.qualityTrace.validators.InspectionValidator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualityInspectionService {
  private final ProductBatchRepository productBatchRepository;
  private final QualityInspectionRepository qualityInspectionRepository;
  private final InspectionItemResultRepository inspectionItemResultRepository;
  private final StatusEventService statusEventService;
  private final AuditService auditService;
  private final InspectionValidator inspectionValidator;

  public QualityInspectionService(
      ProductBatchRepository productBatchRepository,
      QualityInspectionRepository qualityInspectionRepository,
      InspectionItemResultRepository inspectionItemResultRepository,
      StatusEventService statusEventService,
      AuditService auditService,
      InspectionValidator inspectionValidator
  ) {
    this.productBatchRepository = productBatchRepository;
    this.qualityInspectionRepository = qualityInspectionRepository;
    this.inspectionItemResultRepository = inspectionItemResultRepository;
    this.statusEventService = statusEventService;
    this.auditService = auditService;
    this.inspectionValidator = inspectionValidator;
  }

  @Transactional
  public Map<String, Object> submit(QualityInspectionPayload payload, ActorContext actor,
                                    String path) {
    EnumValidator.require(InspectionType.class, payload.inspectionType(), "inspectionType");
    InspectionResultStatus ownResult = inspectionValidator.validateAndSummarize(payload);

    ProductBatch batch = productBatchRepository.lockById(
        productBatchRepository.findByBatchNo(payload.batchNo())
            .orElseThrow(() -> new NotFoundException(
                ErrorMessages.BATCH_NOT_FOUND.formatted(payload.batchNo())))
            .id()
    );
    String previousBatchStatus = statusEventService.latestStatus(
        EntityType.BATCH, batch.id(), batch.batchStatus());
    Instant now = Instant.now();

    Long inspectionId = qualityInspectionRepository.insert(new QualityInspection(
        null,
        batch.id(),
        actor.userId(),
        payload.inspectionType(),
        payload.standardVersion(),
        ownResult.name(),
        previousBatchStatus,
        batch.quantity(),
        now,
        now
    ));

    for (InspectionItemPayload itemPayload : payload.items()) {
      InspectionResultStatus measuredStatus = measuredStatus(itemPayload);
      InspectionResultStatus itemStatus = itemPayload.itemStatus() == null
          || itemPayload.itemStatus().isBlank()
          ? measuredStatus
          : InspectionResultStatus.valueOf(itemPayload.itemStatus());
      inspectionItemResultRepository.insert(new InspectionItemResult(
          null,
          inspectionId,
          itemPayload.itemCode(),
          itemPayload.itemName(),
          itemPayload.measuredValue(),
          itemPayload.limitMin(),
          itemPayload.limitMax(),
          itemStatus.name(),
          now
      ));
    }

    InspectionResultStatus aggregatedResult = aggregateResult(batch.id(), ownResult);
    String nextBatchStatus = nextBatchStatus(previousBatchStatus, aggregatedResult);
    statusEventService.append(EntityType.BATCH, batch.id(), "INSPECTION_SUBMITTED",
        previousBatchStatus, nextBatchStatus, Map.of(
            "inspectionId", inspectionId,
            "inspectionType", payload.inspectionType(),
            "ownResult", ownResult.name(),
            "aggregatedResult", aggregatedResult.name(),
            "previousBatchStatus", previousBatchStatus,
            "receivedBatchQuantity", batch.quantity(),
            "itemCount", payload.items().size()
        ), actor.username());

    for (InspectionItemPayload itemPayload : payload.items()) {
      auditService.record(actor.username(), "SAVE_INSPECTION_ITEM", "INSPECTION",
          inspectionId.toString(),
          LogTemplates.INSPECTION_ITEM_SAVED.formatted(
              inspectionId, itemPayload.itemCode(),
              resolveItemStatus(itemPayload).name()),
          path, "POST");
    }
    auditService.record(actor.username(), "SUBMIT_INSPECTION", "INSPECTION",
        inspectionId.toString(),
        LogTemplates.INSPECTION_CREATED.formatted(
            batch.batchNo(), payload.inspectionType(), ownResult.name(),
            batch.quantity(), previousBatchStatus),
        path, "POST");

    return get(inspectionId);
  }

  public Map<String, Object> get(Long inspectionId) {
    QualityInspection inspection = qualityInspectionRepository.findById(inspectionId)
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.INSPECTION_NOT_FOUND.formatted(inspectionId)));
    List<Map<String, Object>> items = inspectionItemResultRepository
        .findByInspectionId(inspectionId)
        .stream()
        .map(InspectionItemResultDtoFactory::response)
        .toList();
    return QualityInspectionDtoFactory.response(inspection, items);
  }

  public List<Map<String, Object>> listByBatchId(Long batchId) {
    return qualityInspectionRepository.findByBatchId(batchId).stream()
        .map(inspection -> QualityInspectionDtoFactory.response(
            inspection,
            inspectionItemResultRepository.findByInspectionId(inspection.id()).stream()
                .map(InspectionItemResultDtoFactory::response)
                .toList()
        ))
        .toList();
  }

  private InspectionResultStatus aggregateResult(Long batchId, InspectionResultStatus current) {
    InspectionResultStatus aggregated = current;
    for (QualityInspection existing : qualityInspectionRepository.findByBatchId(batchId)) {
      if (existing.resultStatus() != null) {
        InspectionResultStatus previous = InspectionResultStatus.valueOf(existing.resultStatus());
        if (previous.rank() > aggregated.rank()) {
          aggregated = previous;
        }
      }
    }
    return aggregated;
  }

  private String nextBatchStatus(String previousStatus, InspectionResultStatus aggregatedResult) {
    String candidate = switch (aggregatedResult) {
      case PASS -> BatchStatus.PASSED.name();
      case CONDITIONAL_PASS -> BatchStatus.CONDITIONAL_PASS.name();
      case RECHECK -> BatchStatus.RECHECK.name();
      case FAIL -> BatchStatus.ON_HOLD.name();
    };
    return statusRank(previousStatus) > statusRank(candidate) ? previousStatus : candidate;
  }

  private int statusRank(String status) {
    return switch (BatchStatus.valueOf(status)) {
      case CREATED, MATERIAL_ISSUED, IN_PROGRESS -> 0;
      case PASSED -> 1;
      case CONDITIONAL_PASS, RELEASED -> 2;
      case RECHECK -> 3;
      case ON_HOLD -> 4;
      case BLOCKED -> 5;
      case CLOSED -> 6;
    };
  }

  private InspectionResultStatus measuredStatus(InspectionItemPayload item) {
    boolean outOfRange = item.measuredValue().compareTo(item.limitMin()) < 0
        || item.measuredValue().compareTo(item.limitMax()) > 0;
    return outOfRange ? InspectionResultStatus.FAIL : InspectionResultStatus.PASS;
  }

  private InspectionResultStatus resolveItemStatus(InspectionItemPayload item) {
    InspectionResultStatus measured = measuredStatus(item);
    if (item.itemStatus() == null || item.itemStatus().isBlank()) {
      return measured;
    }
    return InspectionResultStatus.valueOf(item.itemStatus());
  }
}
