package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.DefectRecord;
import com.generated.qualityTrace.utils.Formatters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 不良记录响应 DTO 构造器。
 * 关闭后仍带 originalResult / originalSeverity（原判定）与完整事件流（变更前后值）。
 */
public final class DefectRecordDtoFactory {

    private DefectRecordDtoFactory() {}

    public static Map<String, Object> detail(DefectRecord defect, String currentStatus,
                                             List<Map<String, Object>> events) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", defect.getId());
        dto.put("batchId", defect.getBatchId());
        dto.put("defectType", defect.getDefectType());
        dto.put("defectQty", defect.getDefectQty());
        dto.put("severity", defect.getSeverity());
        dto.put("rootCause", defect.getRootCause());
        // 原判定独立字段，处置状态再怎么流转/关闭都不覆盖它
        dto.put("originalResult", defect.getOriginalResult());
        dto.put("originalSeverity", defect.getOriginalSeverity());
        dto.put("createdBy", defect.getCreatedBy());
        dto.put("createdAt", Formatters.dateTime(defect.getCreatedAt()));
        dto.put("dispositionStatus", currentStatus);
        dto.put("dispositionStatusText", Formatters.statusText(currentStatus));
        dto.put("closed", "CLOSED".equals(currentStatus));
        dto.put("events", events);
        return dto;
    }
}
