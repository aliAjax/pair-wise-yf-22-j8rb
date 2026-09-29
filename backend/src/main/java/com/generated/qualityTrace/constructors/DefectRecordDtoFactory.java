package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.DefectRecord;
import com.generated.qualityTrace.utils.Formatters;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefectRecordDtoFactory {
  private DefectRecordDtoFactory() {}

  public static Map<String, Object> response(DefectRecord defect, String currentStatus) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", defect.id());
    dto.put("batchId", defect.batchId());
    dto.put("defectType", defect.defectType());
    dto.put("defectQty", defect.defectQty());
    dto.put("severity", defect.severity());
    dto.put("rootCause", defect.rootCause());
    dto.put("originalJudgment", Map.of(
        "originalDisposition", defect.originalDisposition(),
        "severity", defect.severity(),
        "defectQty", defect.defectQty()
    ));
    dto.put("registeredStatus", defect.dispositionStatus());
    dto.put("currentStatus", currentStatus);
    dto.put("registeredAt", Formatters.instant(defect.registeredAt()));
    dto.put("closedAt", Formatters.instant(defect.closedAt()));
    return dto;
  }
}
