package com.generated.qualityTrace.services;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.constants.EventTypes;
import com.generated.qualityTrace.constants.InspectionResultStatus;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constructors.QualityInspectionDtoFactory;
import com.generated.qualityTrace.exceptions.ValidationDomainException;
import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.models.InspectionItemResult;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.repositories.AppUserRepository;
import com.generated.qualityTrace.repositories.InspectionItemResultRepository;
import com.generated.qualityTrace.repositories.PessimisticLockRepository;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.repositories.QualityInspectionRepository;
import com.generated.qualityTrace.types.InspectionItemResultPayload;
import com.generated.qualityTrace.types.QualityInspectionPayload;
import com.generated.qualityTrace.validators.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QualityInspectionService {

    private static final Logger log = LoggerFactory.getLogger(QualityInspectionService.class);
    private static final String BATCH_AGGREGATE = "BATCH";

    private final QualityInspectionRepository inspectionRepository;
    private final InspectionItemResultRepository itemRepository;
    private final ProductBatchRepository batchRepository;
    private final AppUserRepository userRepository;
    private final PessimisticLockRepository lockRepository;
    private final ProductBatchService batchService;
    private final StateEventService stateEventService;

    public QualityInspectionService(QualityInspectionRepository inspectionRepository,
                                    InspectionItemResultRepository itemRepository,
                                    ProductBatchRepository batchRepository,
                                    AppUserRepository userRepository,
                                    PessimisticLockRepository lockRepository,
                                    ProductBatchService batchService,
                                    StateEventService stateEventService) {
        this.inspectionRepository = inspectionRepository;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.lockRepository = lockRepository;
        this.batchService = batchService;
        this.stateEventService = stateEventService;
    }

    /**
     * 检验整批录入。
     *
     * 并发保证：
     *  1. 事务先 SELECT ... FOR UPDATE 锁住批次行；同批次多条检验同时回传时，
     *     后到的事务排队等待，拿到的是前一个事务提交后的状态与累计数量；
     *  2. 当前状态由 state_event 重放得到，数量在 product_batch 现值上累加（接续，不覆盖）；
     *  3. 主表+检验项+数量回写+事件在同一事务，任何一步抛异常整批回滚，不留半条记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(QualityInspectionPayload payload, Long inspectorUserId,
                                      String idempotencyKey) {
        DomainValidator.requireInspectionType(payload.getInspectionType());
        if (payload.getItems() == null || payload.getItems().isEmpty()) {
            throw new ValidationDomainException("至少提交一个检验项");
        }

        ProductBatch batch = batchService.requireByBatchNo(payload.getBatchNo());

        // 1) 锁批次行——并发回传在此串行
        lockRepository.lockBatch(batch.getId());
        // 拿锁后必须重读：此时读到的一定是“前一批事务提交后”的最新状态与数量
        batch = batchService.requireById(batch.getId());

        // 1.1) 幂等回放：同批次同键重复回传，直接返回首条结果，不重复接续数量
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            java.util.Optional<com.generated.qualityTrace.models.StateEvent> prior =
                    stateEventService.findByIdempotencyKey(
                            BATCH_AGGREGATE, batch.getId(), idempotencyKey);
            if (prior.isPresent()) {
                Object changeObj = stateEventService.eventView(prior.get()).get("change");
                Object inspectionId = changeObj instanceof Map<?, ?> change
                        ? change.get("inspectionId") : null;
                log.info(LogTemplates.INS_REPLAY, inspectionId, idempotencyKey,
                        CurrentUser.username());
                QualityInspection existing =
                        inspectionRepository.selectById(Long.valueOf(String.valueOf(inspectionId)));
                return detail(existing);
            }
        }

        // 2) 重放事件拿到“前一批”的状态（绝不信任调用方传入的状态）
        String beforeState = stateEventService.currentState(BATCH_AGGREGATE, batch.getId());
        if ("QUARANTINED".equals(beforeState)) {
            throw new ValidationDomainException(String.format(
                    "批次 %s 已隔离，不允许再提交检验", batch.getBatchNo()));
        }

        // 3) 逐项判定、汇总结论（任一项写不进去，前面插入的全部回滚）
        List<InspectionItemResult> items = payload.getItems().stream()
                .map(this::buildItem)
                .toList();
        Set<String> itemStatuses = items.stream()
                .map(InspectionItemResult::getItemStatus)
                .collect(Collectors.toSet());
        int recheckQty = payload.getRecheckQty() == null ? 0 : payload.getRecheckQty();
        int defectQty = payload.getDefectQty() == null ? 0 : payload.getDefectQty();
        int qualifiedQty = payload.getQualifiedQty() == null ? 0 : payload.getQualifiedQty();
        InspectionResultStatus result =
                DomainValidator.judgeInspection(itemStatuses, recheckQty);

        // 4) 写检验主表
        QualityInspection inspection = new QualityInspection();
        inspection.setBatchId(batch.getId());
        inspection.setInspectorId(inspectorUserId);
        inspection.setInspectionType(payload.getInspectionType());
        inspection.setStandardVersion(payload.getStandardVersion());
        inspection.setResultStatus(result.name());
        inspection.setInspectedAt(OffsetDateTime.now());
        inspectionRepository.insert(inspection);

        // 5) 逐项写入——任何一项失败，主表随事务一起回滚
        for (InspectionItemResult item : items) {
            item.setInspectionId(inspection.getId());
            itemRepository.insert(item);
            log.info(LogTemplates.INS_ITEM, inspection.getId(), item.getItemCode(),
                    item.getMeasuredValue(), item.getLimitMin(), item.getLimitMax(),
                    item.getItemStatus());
        }

        // 6) 数量接续：在现值上累加，后到检验接住前一批的数量
        int qualifiedBefore = nz(batch.getQualifiedQty());
        int defectBefore = nz(batch.getDefectQty());
        int qualifiedAfter = qualifiedBefore + qualifiedQty;
        int defectAfter = defectBefore + defectQty;
        if (qualifiedAfter + defectAfter > nz(batch.getQuantity())) {
            // 超量：事务回滚，本批检验不留任何记录
            log.warn(LogTemplates.INS_FAIL_TX, batch.getBatchNo(), items.size(),
                    "累计数量超过批次数量", CurrentUser.username());
            throw new ValidationDomainException(String.format(
                    "批次 %s 累计合格 %s + 不良 %s 超过批次数量 %s",
                    batch.getBatchNo(), qualifiedAfter, defectAfter, batch.getQuantity()));
        }
        batch.setQualifiedQty(qualifiedAfter);
        batch.setDefectQty(defectAfter);
        batchRepository.updateById(batch);

        // 7) 追加状态事件（保留每次状态变化；即便状态码不变，也留检验事件）
        String toState = mapBatchState(result);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("inspectionId", inspection.getId());
        summary.put("inspectionType", payload.getInspectionType());
        summary.put("result", result.name());
        summary.put("qualifiedQtyBefore", qualifiedBefore);
        summary.put("qualifiedQtyAfter", qualifiedAfter);
        summary.put("defectQtyBefore", defectBefore);
        summary.put("defectQtyAfter", defectAfter);
        summary.put("addedQualified", qualifiedQty);
        summary.put("addedDefect", defectQty);
        stateEventService.append(BATCH_AGGREGATE, batch.getId(),
                eventTypeOf(result), beforeState, toState, summary, idempotencyKey);

        log.info(LogTemplates.BATCH_QUANTITY, batch.getBatchNo(),
                qualifiedBefore, qualifiedAfter, defectBefore, defectAfter,
                CurrentUser.username());
        log.info(LogTemplates.INS_SUBMIT, batch.getBatchNo(), payload.getInspectionType(),
                payload.getStandardVersion(), items.size(), result.name(),
                CurrentUser.username());

        return detail(inspection);
    }

    private InspectionItemResult buildItem(InspectionItemResultPayload p) {
        InspectionItemResult item = new InspectionItemResult();
        item.setItemCode(p.getItemCode());
        item.setItemName(p.getItemName());
        item.setMeasuredValue(p.getMeasuredValue());
        item.setLimitMin(p.getLimitMin());
        item.setLimitMax(p.getLimitMax());
        // 以实测值与限值的判定为准；调用方传入状态只在显式 RECHECK 时采纳
        String judged = DomainValidator.judgeItem(p.getMeasuredValue(), p.getLimitMin(), p.getLimitMax());
        item.setItemStatus("RECHECK".equalsIgnoreCase(p.getItemStatus()) ? "RECHECK" : judged);
        return item;
    }

    private String mapBatchState(InspectionResultStatus result) {
        return switch (result) {
            case PASS -> "INSPECTED_PASS";
            case FAIL -> "INSPECTED_FAIL";
            case CONDITIONAL_PASS -> "CONDITIONAL_PASS";
            case RECHECK -> "RECHECK_PENDING";
        };
    }

    private String eventTypeOf(InspectionResultStatus result) {
        return switch (result) {
            case PASS -> EventTypes.INSPECTION_PASSED;
            case FAIL -> EventTypes.INSPECTION_FAILED;
            case CONDITIONAL_PASS -> EventTypes.INSPECTION_CONDITIONAL;
            case RECHECK -> EventTypes.INSPECTION_RECHECK;
        };
    }

    public Map<String, Object> detail(QualityInspection inspection) {
        List<InspectionItemResult> items = itemRepository.selectList(
                new QueryWrapper<InspectionItemResult>()
                        .eq("inspection_id", inspection.getId())
                        .orderByAsc("id"));
        AppUser inspector = userRepository.selectById(inspection.getInspectorId());
        return QualityInspectionDtoFactory.inspection(inspection,
                inspector == null ? null : inspector.getDisplayName(),
                items.stream().map(QualityInspectionDtoFactory::item).toList());
    }

    /** 按批次列出历次检验（追溯树使用）。 */
    public List<Map<String, Object>> listByBatchId(Long batchId) {
        return inspectionRepository.selectList(
                        new QueryWrapper<QualityInspection>()
                                .eq("batch_id", batchId)
                                .orderByAsc("id")).stream()
                .map(this::detail)
                .toList();
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
