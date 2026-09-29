package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.BatchStatus;
import com.generated.qualityTrace.constants.DefectDispositionStatus;
import com.generated.qualityTrace.constants.DefectSeverity;
import com.generated.qualityTrace.constants.EntityType;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constructors.DefectRecordDtoFactory;
import com.generated.qualityTrace.constructors.StatusEventDtoFactory;
import com.generated.qualityTrace.exceptions.IllegalBusinessStateException;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.exceptions.ValidationException;
import com.generated.qualityTrace.models.DefectRecord;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.StatusEvent;
import com.generated.qualityTrace.repositories.DefectRecordRepository;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.CloseDefectPayload;
import com.generated.qualityTrace.types.DefectDispositionPayload;
import com.generated.qualityTrace.types.DefectRecordPayload;
import com.generated.qualityTrace.validators.EnumValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefectRecordService {
  private final ProductBatchRepository productBatchRepository;
  private final DefectRecordRepository defectRecordRepository;
  private final StatusEventService statusEventService;
  private final AuditService auditService;

  public DefectRecordService(
      ProductBatchRepository productBatchRepository,
      DefectRecordRepository defectRecordRepository,
      StatusEventService statusEventService,
      AuditService auditService
  ) {
    this.productBatchRepository = productBatchRepository;
    this.defectRecordRepository = defectRecordRepository;
    this.statusEventService = statusEventService;
    this.auditService = auditService;
  }

  @Transactional
  public Map<String, Object> register(DefectRecordPayload payload, ActorContext actor,
                                      String path) {
    EnumValidator.require(DefectSeverity.class, payload.severity(), "severity");
    ProductBatch batch = productBatchRepository.lockById(
        productBatchRepository.findByBatchNo(payload.batchNo())
            .orElseThrow(() -> new NotFoundException(
                ErrorMessages.BATCH_NOT_FOUND.formatted(payload.batchNo())))
            .id()
    );
    if (defectRecordRepository.sumOpenDefectQuantity(batch.id())
        .add(payload.defectQty()).compareTo(batch.quantity()) > 0) {
      throw new ValidationException(
          ErrorMessages.DEFECT_QTY_EXCEEDS_BATCH.formatted(payload.defectQty(), batch.quantity()));
    }

    String previousBatchStatus = statusEventService.latestStatus(
        EntityType.BATCH, batch.id(), batch.batchStatus());
    Instant now = Instant.now();
    Long defectId = defectRecordRepository.insert(new DefectRecord(
        null,
        batch.id(),
        payload.defectType(),
        payload.defectQty(),
        payload.severity(),
        payload.rootCause(),
        DefectDispositionStatus.OPEN.name(),
        DefectDispositionStatus.OPEN.name(),
        now,
        null,
        now,
        now
    ));

    statusEventService.append(EntityType.DEFECT, defectId, "DEFECT_REGISTERED", null,
        DefectDispositionStatus.OPEN.name(), Map.of(
            "defectType", payload.defectType(),
            "defectQty", payload.defectQty(),
            "severity", payload.severity(),
            "rootCause", payload.rootCause()
        ), actor.username());

    String nextBatchStatus = nextStatusAfterRegistration(
        previousBatchStatus, DefectSeverity.valueOf(payload.severity()));
    statusEventService.append(EntityType.BATCH, batch.id(), "DEFECT_REGISTERED",
        previousBatchStatus, nextBatchStatus, Map.of(
            "defectId", defectId,
            "severity", payload.severity(),
            "defectQty", payload.defectQty()
        ), actor.username());

    auditService.record(actor.username(), "REGISTER_DEFECT", "DEFECT", defectId.toString(),
        LogTemplates.DEFECT_REGISTERED.formatted(
            batch.batchNo(), payload.defectType(), payload.severity(), payload.defectQty()),
        path, "POST");
    return get(defectId);
  }

  @Transactional
  public Map<String, Object> dispose(Long defectId, DefectDispositionPayload payload,
                                     ActorContext actor, String path) {
    EnumValidator.require(DefectDispositionStatus.class, payload.dispositionStatus(),
        "dispositionStatus");
    DefectDispositionStatus target = DefectDispositionStatus.valueOf(payload.dispositionStatus());
    if (target == DefectDispositionStatus.OPEN || target == DefectDispositionStatus.CLOSED
        || target == DefectDispositionStatus.IN_DISPOSITION) {
      throw new ValidationException("dispositionStatus 必须是明确处置结论：REWORK/SCRAP/CONCESSION/RETURN_TO_SUPPLIER");
    }

    ProductBatch batch = lockBatchForDefect(defectId);
    DefectRecord defect = defectRecordRepository.lockById(defectId);
    String fromDefectStatus = currentDefectStatus(defect);
    if (DefectDispositionStatus.CLOSED.name().equals(fromDefectStatus)) {
      throw new IllegalBusinessStateException(ErrorMessages.DEFECT_ALREADY_CLOSED);
    }

    statusEventService.append(EntityType.DEFECT, defectId, "DEFECT_DISPOSITION_DECIDED",
        fromDefectStatus, target.name(), Map.of(
            "previousValue", fromDefectStatus,
            "nextValue", target.name(),
            "note", payload.note() == null ? "" : payload.note()
        ), actor.username());

    String previousBatchStatus = statusEventService.latestStatus(
        EntityType.BATCH, batch.id(), batch.batchStatus());
    String nextBatchStatus = strongerStatus(
        previousBatchStatus, batchStatusForDisposition(target));
    statusEventService.append(EntityType.BATCH, batch.id(), "DEFECT_DISPOSITION_DECIDED",
        previousBatchStatus, nextBatchStatus, Map.of(
            "defectId", defectId,
            "previousValue", fromDefectStatus,
            "nextValue", target.name()
        ), actor.username());

    auditService.record(actor.username(), "DISPOSE_DEFECT", "DEFECT", defectId.toString(),
        LogTemplates.DEFECT_DISPOSITION_CHANGED.formatted(defectId, fromDefectStatus, target),
        path, "POST");
    return get(defectId);
  }

  @Transactional
  public Map<String, Object> close(Long defectId, CloseDefectPayload payload,
                                   ActorContext actor, String path) {
    ProductBatch batch = lockBatchForDefect(defectId);
    DefectRecord defect = defectRecordRepository.lockById(defectId);
    String fromDefectStatus = currentDefectStatus(defect);
    if (DefectDispositionStatus.CLOSED.name().equals(fromDefectStatus)) {
      throw new IllegalBusinessStateException(ErrorMessages.DEFECT_ALREADY_CLOSED);
    }
    if (DefectDispositionStatus.OPEN.name().equals(fromDefectStatus)
        || DefectDispositionStatus.IN_DISPOSITION.name().equals(fromDefectStatus)) {
      throw new IllegalBusinessStateException(ErrorMessages.DEFECT_MUST_DISPOSE_BEFORE_CLOSE);
    }

    DefectDispositionStatus dispositionBeforeClose = DefectDispositionStatus.valueOf(fromDefectStatus);
    statusEventService.append(EntityType.DEFECT, defectId, "DEFECT_CLOSED",
        fromDefectStatus, DefectDispositionStatus.CLOSED.name(), Map.of(
            "originalJudgment", Map.of(
                "originalDisposition", defect.originalDisposition(),
                "severity", defect.severity(),
                "defectQty", defect.defectQty()
            ),
            "beforeValue", fromDefectStatus,
            "afterValue", DefectDispositionStatus.CLOSED.name(),
            "note", payload.note() == null ? "" : payload.note()
        ), actor.username());
    defectRecordRepository.markClosed(defectId);

    String previousBatchStatus = statusEventService.latestStatus(
        EntityType.BATCH, batch.id(), batch.batchStatus());
    boolean otherOpenDefects = defectRecordRepository.findByBatchId(batch.id()).stream()
        .filter(other -> !other.id().equals(defectId))
        .map(this::currentDefectStatus)
        .filter(status -> !DefectDispositionStatus.CLOSED.name().equals(status))
        .findAny()
        .isPresent();
    String nextBatchStatus = otherOpenDefects
        ? strongerStatus(previousBatchStatus, BatchStatus.ON_HOLD.name())
        : batchStatusAfterAllClosed(batch.id());
    statusEventService.append(EntityType.BATCH, batch.id(), "DEFECT_CLOSED",
        previousBatchStatus, nextBatchStatus, Map.of(
            "defectId", defectId,
            "closedDisposition", dispositionBeforeClose.name(),
            "otherOpenDefects", otherOpenDefects
        ), actor.username());

    auditService.record(actor.username(), "CLOSE_DEFECT", "DEFECT", defectId.toString(),
        LogTemplates.DEFECT_CLOSED.formatted(
            defectId, defect.originalDisposition(), dispositionBeforeClose.name()),
        path, "POST");
    return get(defectId);
  }

  public Map<String, Object> get(Long defectId) {
    DefectRecord defect = defectRecordRepository.findById(defectId)
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.DEFECT_NOT_FOUND.formatted(defectId)));
    return DefectRecordDtoFactory.response(defect, currentDefectStatus(defect));
  }

  public List<Map<String, Object>> listByBatchId(Long batchId) {
    return defectRecordRepository.findByBatchId(batchId).stream()
        .map(defect -> DefectRecordDtoFactory.response(defect, currentDefectStatus(defect)))
        .toList();
  }

  public List<Map<String, Object>> events(Long defectId) {
    get(defectId);
    return statusEventService.findByEntity(EntityType.DEFECT, defectId).stream()
        .map(StatusEventDtoFactory::response)
        .toList();
  }

  private ProductBatch lockBatchForDefect(Long defectId) {
    DefectRecord defect = defectRecordRepository.findById(defectId)
        .orElseThrow(() -> new NotFoundException(
            ErrorMessages.DEFECT_NOT_FOUND.formatted(defectId)));
    ProductBatch batch = productBatchRepository.lockById(defect.batchId());
    if (batch == null) {
      throw new NotFoundException(ErrorMessages.BATCH_NOT_FOUND.formatted(defect.batchId()));
    }
    return batch;
  }

  private String currentDefectStatus(DefectRecord defect) {
    List<StatusEvent> events = statusEventService.findByEntity(EntityType.DEFECT, defect.id());
    return events.isEmpty() ? defect.dispositionStatus() : events.get(events.size() - 1).toStatus();
  }

  private String nextStatusAfterRegistration(String previousBatchStatus,
                                             DefectSeverity severity) {
    String candidate = severity == DefectSeverity.CRITICAL
        ? BatchStatus.BLOCKED.name()
        : BatchStatus.ON_HOLD.name();
    return strongerStatus(previousBatchStatus, candidate);
  }

  private String batchStatusForDisposition(DefectDispositionStatus disposition) {
    return switch (disposition) {
      case SCRAP, RETURN_TO_SUPPLIER -> BatchStatus.BLOCKED.name();
      case REWORK, CONCESSION -> BatchStatus.ON_HOLD.name();
      default -> BatchStatus.ON_HOLD.name();
    };
  }

  private String batchStatusAfterAllClosed(Long batchId) {
    String derived = BatchStatus.PASSED.name();
    for (DefectRecord defect : defectRecordRepository.findByBatchId(batchId)) {
      DefectDispositionStatus finalDisposition =
          DefectDispositionStatus.valueOf(currentDefectStatus(defect));
      String candidate = switch (finalDisposition) {
        case REWORK -> BatchStatus.PASSED.name();
        case CONCESSION -> BatchStatus.RELEASED.name();
        case SCRAP, RETURN_TO_SUPPLIER -> BatchStatus.CLOSED.name();
        default -> BatchStatus.ON_HOLD.name();
      };
      derived = strongerStatus(derived, candidate);
    }
    return derived;
  }

  private String strongerStatus(String left, String right) {
    return batchRank(left) >= batchRank(right) ? left : right;
  }

  private int batchRank(String status) {
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
}
