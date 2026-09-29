package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.repositories.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批次全链路追溯：工单开工 -> 批次投料 -> 历次检验(含检验项) -> 不良登记与处置。
 * 所有“状态”均来自 state_event 重放，不读任何可覆盖的状态列。
 */
@Service
public class TraceService {

    private static final Logger log = LoggerFactory.getLogger(TraceService.class);

    private final ProductBatchService batchService;
    private final WorkOrderRepository workOrderRepository;
    private final QualityInspectionService inspectionService;
    private final DefectRecordService defectService;
    private final StateEventService stateEventService;

    public TraceService(ProductBatchService batchService,
                        WorkOrderRepository workOrderRepository,
                        QualityInspectionService inspectionService,
                        DefectRecordService defectService,
                        StateEventService stateEventService) {
        this.batchService = batchService;
        this.workOrderRepository = workOrderRepository;
        this.inspectionService = inspectionService;
        this.defectService = defectService;
        this.stateEventService = stateEventService;
    }

    public Map<String, Object> trace(String batchNo) {
        ProductBatch batch = batchService.requireByBatchNo(batchNo);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("batchNo", batch.getBatchNo());
        root.put("materialLotNo", batch.getMaterialLotNo());
        root.put("quantity", batch.getQuantity());
        root.put("qualifiedQty", batch.getQualifiedQty());
        root.put("defectQty", batch.getDefectQty());
        root.put("producedAt", batch.getProducedAt());
        root.put("currentStatus", batchService.currentStatus(batch.getId()));

        // 批次事件流（投料/检验/不良/放行，每次状态变化都在）
        List<Map<String, Object>> batchEvents =
                stateEventService.streamView("BATCH", batch.getId());
        root.put("events", batchEvents);

        // 工单开工链路（旧工单无批次时不在这里出现，但工单接口仍可单独查）
        Map<String, Object> workOrderNode = null;
        if (batch.getWorkOrderId() != null) {
            WorkOrder order = workOrderRepository.selectById(batch.getWorkOrderId());
            if (order != null) {
                workOrderNode = new LinkedHashMap<>();
                workOrderNode.put("orderNo", order.getOrderNo());
                workOrderNode.put("productCode", order.getProductCode());
                workOrderNode.put("productName", order.getProductName());
                workOrderNode.put("plannedQty", order.getPlannedQty());
                workOrderNode.put("completedQty", order.getCompletedQty());
                workOrderNode.put("lineCode", order.getLineCode());
                workOrderNode.put("startAt", order.getStartAt());
                workOrderNode.put("currentStatus",
                        stateEventService.currentState("WORK_ORDER", order.getId()));
                workOrderNode.put("events",
                        stateEventService.streamView("WORK_ORDER", order.getId()));
            }
        }
        root.put("workOrder", workOrderNode);

        // 检验链路
        List<Map<String, Object>> inspections = inspectionService.listByBatchId(batch.getId());
        root.put("inspections", inspections);

        // 不良链路（含关闭后的原判定与全部前后值）
        List<Map<String, Object>> defects = defectService.listByBatchId(batch.getId());
        root.put("defects", defects);

        log.info(LogTemplates.TRACE_QUERY, batchNo, batchEvents.size(),
                inspections.size(), defects.size(), CurrentUser.username());
        return root;
    }
}
