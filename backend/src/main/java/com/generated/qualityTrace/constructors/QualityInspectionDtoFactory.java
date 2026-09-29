package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.InspectionItemResult;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.utils.Formatters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 检验及检验项响应 DTO 构造器。 */
public final class QualityInspectionDtoFactory {

    private QualityInspectionDtoFactory() {}

    public static Map<String, Object> inspection(QualityInspection inspection,
                                                 String inspectorName,
                                                 List<Map<String, Object>> items) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", inspection.getId());
        dto.put("batchId", inspection.getBatchId());
        dto.put("inspectorId", inspection.getInspectorId());
        dto.put("inspectorName", inspectorName);
        dto.put("inspectionType", inspection.getInspectionType());
        dto.put("standardVersion", inspection.getStandardVersion());
        dto.put("resultStatus", inspection.getResultStatus());
        dto.put("resultStatusText", Formatters.statusText(inspection.getResultStatus()));
        dto.put("inspectedAt", Formatters.dateTime(inspection.getInspectedAt()));
        dto.put("items", items);
        return dto;
    }

    public static Map<String, Object> item(InspectionItemResult item) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", item.getId());
        dto.put("inspectionId", item.getInspectionId());
        dto.put("itemCode", item.getItemCode());
        dto.put("itemName", item.getItemName());
        dto.put("measuredValue", Formatters.measured(item.getMeasuredValue()));
        dto.put("limitMin", Formatters.measured(item.getLimitMin()));
        dto.put("limitMax", Formatters.measured(item.getLimitMax()));
        dto.put("itemStatus", item.getItemStatus());
        return dto;
    }
}
