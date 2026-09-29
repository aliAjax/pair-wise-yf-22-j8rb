package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.WorkOrder;
import com.generated.qualityTrace.utils.Formatters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工单响应 DTO 构造器：controller/service 不散写响应结构。 */
public final class WorkOrderDtoFactory {

    private WorkOrderDtoFactory() {}

    public static Map<String, Object> detail(WorkOrder order, String currentStatus,
                                             List<Map<String, Object>> events) {
        Map<String, Object> dto = base(order);
        dto.put("status", currentStatus);
        dto.put("statusText", Formatters.statusText(currentStatus));
        dto.put("events", events);
        dto.put("hasBatch", false); // 由 service 用实际数据覆盖
        return dto;
    }

    public static Map<String, Object> listItem(WorkOrder order, String currentStatus) {
        Map<String, Object> dto = base(order);
        dto.put("status", currentStatus);
        dto.put("statusText", Formatters.statusText(currentStatus));
        return dto;
    }

    private static Map<String, Object> base(WorkOrder order) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", order.getId());
        dto.put("orderNo", order.getOrderNo());
        dto.put("productCode", order.getProductCode());
        dto.put("productName", order.getProductName());
        dto.put("plannedQty", order.getPlannedQty());
        dto.put("completedQty", order.getCompletedQty());
        dto.put("lineCode", order.getLineCode());
        dto.put("startAt", Formatters.dateTime(order.getStartAt()));
        dto.put("createdAt", Formatters.dateTime(order.getCreatedAt()));
        return dto;
    }
}
