package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.DefectSeverity;
import com.generated.qualityTrace.constants.EventTypes;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constructors.DefectRecordDtoFactory;
import com.generated.qualityTrace.exceptions.ConflictException;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.exceptions.ValidationDomainException;
import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.models.DefectRecord;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.repositories.DefectRecordRepository;
import com.generated.qualityTrace.repositories.PessimisticLockRepository;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.validators.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DefectRecordService {

    private static final Logger log = LoggerFactory.getLogger(DefectRecordService.class);
    private static final String DEFECT_AGGREGATE = "DEFECT";
    private static final String BATCH_AGGREGATE = "BATCH";

    /** 处置状态机：键=当前状态，值=允许流转到的状态。 */
    private static final Map<String, Set<String>> DISPOSITION_FLOW = Map.of(
            "OPEN", Set.of("IN_DISPOSITION"),
            "IN_DISPOSITION", Set.of("REWORKING", "CONCESSION", "SCRAPPED"),
            "REWORKING", Set.of("IN_DISPOSITION", "CLOSED"),
            "CONCESSION", Set.of("CLOSED"),
            "SCRAPPED", Set.of("CLOSED"));

    private final DefectRecordRepository defectRepository;
    private final ProductBatchRepository batchRepository;
    private final PessimisticLockRepository lockRepository;
    private final ProductBatchService batchService;
    private final StateEventService stateEventService;

    public DefectRecordService(DefectRecordRepository defectRepository,
                               ProductBatchRepository batchRepository,
                               PessimisticLockRepository lockRepository,
                               ProductBatchService batchService,
                               StateEventService stateEventService) {
        this.defectRepository = defectRepository;
        this.batchRepository = batchRepository;
        this.lockRepository = lockRepository;
        this.batchService = batchService;
        this.stateEventService = stateEventService;
    }

    /**
     * 登记不良。原判定（originalResult / severity）落库即不可变；
     * 同事务回写批次不良数量并追加两条事件（批次冻结 + 不良待处置）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(String batchNo, String defectType, Integer defectQty,
                                        String severity, String rootCause,
                                        String originalResult) {
        DomainValidator.requireSeverity(severity);
        DomainValidator.requireOriginalResult(originalResult);
        if ("CONCESSION".equals(originalResult)
                && DefectSeverity.valueOf(severity) == DefectSeverity.CRITICAL) {
            throw new ValidationDomainException("CRITICAL 严重度不允许直接让步接收");
        }

        ProductBatch batch = batchService.requireByBatchNo(batchNo);
        lockRepository.lockBatch(batch.getId());
        // 拿锁后重读，接住此前检验/不良事务提交的数量
        batch = batchService.requireById(batch.getId());

        int defectBefore = nz(batch.getDefectQty());
        int defectAfter = defectBefore + defectQty;
        if (defectAfter + nz(batch.getQualifiedQty()) > nz(batch.getQuantity())) {
            throw new ConflictException("DEFECT_QTY_EXCEEDED", String.format(
                    "不良数量 %s 超过批次可计不良余量 %s",
                    defectQty, nz(batch.getQuantity())
                            - nz(batch.getQualifiedQty()) - defectBefore));
        }

        DefectRecord defect = new DefectRecord();
        defect.setBatchId(batch.getId());
        defect.setDefectType(defectType);
        defect.setDefectQty(defectQty);
        defect.setSeverity(severity);
        defect.setRootCause(rootCause);
        defect.setOriginalResult(originalResult);
        defect.setOriginalSeverity(severity);
        defect.setCreatedBy(CurrentUser.username());
        defectRepository.insert(defect);

        batch.setDefectQty(defectAfter);
        batchRepository.updateById(batch);

        String batchBefore = stateEventService.currentState(BATCH_AGGREGATE, batch.getId());
        Map<String, Object> batchSummary = new LinkedHashMap<>();
        batchSummary.put("defectId", defect.getId());
        batchSummary.put("defectQtyBefore", defectBefore);
        batchSummary.put("defectQtyAfter", defectAfter);
        batchSummary.put("addedDefect", defectQty);
        stateEventService.append(BATCH_AGGREGATE, batch.getId(),
                EventTypes.DEFECT_REGISTERED, batchBefore, "BLOCKED", batchSummary);

        Map<String, Object> defectSummary = new LinkedHashMap<>();
        defectSummary.put("originalResult", originalResult);
        defectSummary.put("severity", severity);
        defectSummary.put("defectQty", defectQty);
        stateEventService.append(DEFECT_AGGREGATE, defect.getId(),
                EventTypes.DEFECT_REGISTERED, null, "OPEN", defectSummary);

        log.info(LogTemplates.DEF_REGISTER, batchNo, defectType, defectQty, severity,
                originalResult, CurrentUser.username());
        return detail(defect.getId());
    }

    /**
     * 处置流转：OPEN -> IN_DISPOSITION -> REWORKING/CONCESSION/SCRAPPED -> CLOSED。
     * 每次流转的前后值都写进 state_event；不改 defect_record 的任何判定字段。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dispose(Long defectId, String targetStatus, String note) {
        DefectRecord defect = requireDefect(defectId);
        lockRepository.lockDefect(defectId);

        String from = stateEventService.currentState(DEFECT_AGGREGATE, defectId);
        Set<String> allowed = DISPOSITION_FLOW.getOrDefault(from, Set.of());
        if (!allowed.contains(targetStatus)) {
            log.info(LogTemplates.DEF_REJECT, defectId, from, targetStatus,
                    CurrentUser.username());
            throw new ConflictException("DEFECT_TRANSITION_DENIED", String.format(
                    "不良处置不允许从 %s 变更为 %s", from, targetStatus));
        }
        if ("CONCESSION".equals(targetStatus)
                && DefectSeverity.valueOf(defect.getOriginalSeverity()) == DefectSeverity.CRITICAL) {
            throw new ValidationDomainException("CRITICAL 严重度不允许让步放行");
        }
        if ("CLOSED".equals(targetStatus) && (note == null || note.isBlank())) {
            throw new ValidationDomainException("关闭不良处置必须填写关闭结论");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dispositionBefore", from);
        summary.put("dispositionAfter", targetStatus);
        summary.put("note", note);
        // 同时带上原判定，关闭后仅凭事件流也能看到“原判定是什么、后来怎么变的”
        summary.put("originalResult", defect.getOriginalResult());
        summary.put("originalSeverity", defect.getOriginalSeverity());

        String eventType = "CLOSED".equals(targetStatus)
                ? EventTypes.DEFECT_CLOSED
                : EventTypes.DEFECT_DISPOSITION_CHANGED;
        stateEventService.append(DEFECT_AGGREGATE, defectId, eventType, from, targetStatus, summary);

        if ("CLOSED".equals(targetStatus)) {
            log.info(LogTemplates.DEF_CLOSE, defectId, targetStatus,
                    defect.getOriginalResult(), defect.getOriginalSeverity(),
                    CurrentUser.username());
        } else {
            log.info(LogTemplates.DEF_DISPOSE, defectId, from, targetStatus,
                    from, targetStatus, CurrentUser.username());
        }
        return detail(defectId);
    }

    /** 不良详情：关闭后仍返回原判定与每次变更前后值（事件流）。 */
    public Map<String, Object> detail(Long defectId) {
        DefectRecord defect = requireDefect(defectId);
        String status = stateEventService.currentState(DEFECT_AGGREGATE, defectId);
        List<Map<String, Object>> events = stateEventService.streamView(DEFECT_AGGREGATE, defectId);
        return DefectRecordDtoFactory.detail(defect, status, events);
    }

    public List<Map<String, Object>> listByBatchId(Long batchId) {
        return defectRepository.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DefectRecord>()
                                .eq("batch_id", batchId)
                                .orderByAsc("id")).stream()
                .map(d -> detail(d.getId()))
                .toList();
    }

    private DefectRecord requireDefect(Long id) {
        DefectRecord defect = defectRepository.selectById(id);
        if (defect == null) {
            throw NotFoundException.defect(id);
        }
        return defect;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
