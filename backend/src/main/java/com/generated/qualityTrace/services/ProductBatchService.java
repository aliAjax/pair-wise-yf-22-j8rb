package com.generated.qualityTrace.services;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.constants.EventTypes;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constructors.ProductBatchDtoFactory;
import com.generated.qualityTrace.exceptions.ConflictException;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.repositories.ProductBatchRepository;
import com.generated.qualityTrace.repositories.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProductBatchService {

    private static final Logger log = LoggerFactory.getLogger(ProductBatchService.class);
    private static final String AGGREGATE = "BATCH";

    private final ProductBatchRepository productBatchRepository;
    private final WorkOrderRepository workOrderRepository;
    private final StateEventService stateEventService;
    private final WorkOrderService workOrderService;

    public ProductBatchService(ProductBatchRepository productBatchRepository,
                               WorkOrderRepository workOrderRepository,
                               StateEventService stateEventService,
                               WorkOrderService workOrderService) {
        this.productBatchRepository = productBatchRepository;
        this.workOrderRepository = workOrderRepository;
        this.stateEventService = stateEventService;
        this.workOrderService = workOrderService;
    }

    /**
     * 批次投料（创建批次）。
     * workOrderId 可空：旧工单/补录场景不强制关联。
     * 关联工单时同事务回写工单进度。
     */
    @Transactional
    public Map<String, Object> create(String batchNo, Long workOrderId, Integer quantity,
                                      String materialLotNo) {
        Long dup = productBatchRepository.selectCount(
                new QueryWrapper<ProductBatch>().eq("batch_no", batchNo));
        if (dup != null && dup > 0) {
            throw new ConflictException("BATCH_NO_CONFLICT", "批次号已存在：" + batchNo);
        }

        String orderNo = null;
        if (workOrderId != null) {
            WorkOrder order = workOrderRepository.selectById(workOrderId);
            if (order == null) {
                throw NotFoundException.workOrder(String.valueOf(workOrderId));
            }
            orderNo = order.getOrderNo();
        }

        ProductBatch batch = new ProductBatch();
        batch.setBatchNo(batchNo);
        batch.setWorkOrderId(workOrderId);
        batch.setQuantity(quantity);
        batch.setQualifiedQty(0);
        batch.setDefectQty(0);
        batch.setMaterialLotNo(materialLotNo);
        productBatchRepository.insert(batch);

        if (workOrderId != null) {
            workOrderService.addProgress(workOrderId, quantity);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("batchNo", batchNo);
        summary.put("quantity", quantity);
        summary.put("workOrderId", workOrderId);
        summary.put("materialLotNo", materialLotNo);
        stateEventService.append(AGGREGATE, batch.getId(), EventTypes.BATCH_CREATED,
                null, "PRODUCED", summary);

        log.info(LogTemplates.BATCH_CREATE, batchNo, workOrderId, quantity,
                materialLotNo, CurrentUser.username());
        return ProductBatchDtoFactory.detail(batch, "PRODUCED", workOrderId, orderNo);
    }

    /** 按批次号取实体（不存在抛 404）。 */
    public ProductBatch requireByBatchNo(String batchNo) {
        ProductBatch batch = productBatchRepository.selectOne(
                new QueryWrapper<ProductBatch>().eq("batch_no", batchNo));
        if (batch == null) {
            throw NotFoundException.batch(batchNo);
        }
        return batch;
    }

    public ProductBatch requireById(Long id) {
        ProductBatch batch = productBatchRepository.selectById(id);
        if (batch == null) {
            throw NotFoundException.batch(String.valueOf(id));
        }
        return batch;
    }

    public Map<String, Object> detailByBatchNo(String batchNo) {
        ProductBatch batch = requireByBatchNo(batchNo);
        String status = stateEventService.currentState(AGGREGATE, batch.getId());
        String orderNo = resolveOrderNo(batch.getWorkOrderId());
        return ProductBatchDtoFactory.detail(batch, status, batch.getWorkOrderId(), orderNo);
    }

    public String resolveOrderNo(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        WorkOrder order = workOrderRepository.selectById(workOrderId);
        return order == null ? null : order.getOrderNo();
    }

    public String currentStatus(Long batchId) {
        return stateEventService.currentState(AGGREGATE, batchId);
    }
}
