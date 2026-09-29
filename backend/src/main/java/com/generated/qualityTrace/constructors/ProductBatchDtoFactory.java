package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.utils.Formatters;

import java.util.LinkedHashMap;
import java.util.Map;

/** 批次响应 DTO 构造器。 */
public final class ProductBatchDtoFactory {

    private ProductBatchDtoFactory() {}

    public static Map<String, Object> detail(ProductBatch batch, String currentStatus,
                                             Long workOrderId, String orderNo) {
        Map<String, Object> dto = base(batch);
        dto.put("workOrderId", workOrderId);
        dto.put("orderNo", orderNo); // 旧工单/无关联时为 null，不强制回填
        dto.put("status", currentStatus);
        dto.put("statusText", Formatters.statusText(currentStatus));
        return dto;
    }

    public static Map<String, Object> base(ProductBatch batch) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", batch.getId());
        dto.put("batchNo", batch.getBatchNo());
        dto.put("workOrderId", batch.getWorkOrderId());
        dto.put("quantity", batch.getQuantity());
        dto.put("qualifiedQty", batch.getQualifiedQty());
        dto.put("defectQty", batch.getDefectQty());
        dto.put("materialLotNo", batch.getMaterialLotNo());
        dto.put("producedAt", Formatters.dateTime(batch.getProducedAt()));
        return dto;
    }
}
