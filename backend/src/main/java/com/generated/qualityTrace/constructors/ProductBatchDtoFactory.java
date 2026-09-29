package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.utils.Formatters;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProductBatchDtoFactory {
  private ProductBatchDtoFactory() {}

  public static Map<String, Object> response(ProductBatch batch, String currentStatus) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", batch.id());
    dto.put("batchNo", batch.batchNo());
    dto.put("workOrderId", batch.workOrderId());
    dto.put("quantity", batch.quantity());
    dto.put("materialLotNo", batch.materialLotNo());
    dto.put("producedAt", Formatters.instant(batch.producedAt()));
    dto.put("registeredStatus", batch.batchStatus());
    dto.put("currentStatus", currentStatus);
    dto.put("createdAt", Formatters.instant(batch.createdAt()));
    dto.put("updatedAt", Formatters.instant(batch.updatedAt()));
    return dto;
  }
}
