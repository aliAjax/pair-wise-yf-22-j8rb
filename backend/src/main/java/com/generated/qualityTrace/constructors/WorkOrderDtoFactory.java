package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.utils.Formatters;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkOrderDtoFactory {
  private WorkOrderDtoFactory() {}

  public static Map<String, Object> response(WorkOrder workOrder, String currentStatus,
                                             BigDecimal completedQty, BigDecimal remainingQty) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", workOrder.id());
    dto.put("orderNo", workOrder.orderNo());
    dto.put("productCode", workOrder.productCode());
    dto.put("productName", workOrder.productName());
    dto.put("plannedQty", workOrder.plannedQty());
    dto.put("lineCode", workOrder.lineCode());
    dto.put("startAt", Formatters.instant(workOrder.startAt()));
    dto.put("registeredStatus", workOrder.status());
    dto.put("currentStatus", currentStatus);
    dto.put("completedQty", completedQty);
    dto.put("remainingQty", remainingQty);
    dto.put("createdAt", Formatters.instant(workOrder.createdAt()));
    dto.put("updatedAt", Formatters.instant(workOrder.updatedAt()));
    return dto;
  }
}
