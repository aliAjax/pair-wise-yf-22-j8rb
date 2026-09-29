package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.utils.Formatters;
import java.util.List;
import java.util.Map;

public final class QualityInspectionDtoFactory {
  private QualityInspectionDtoFactory() {}

  public static Map<String, Object> response(QualityInspection inspection,
                                             List<Map<String, Object>> items) {
    return Map.of(
        "id", inspection.id(),
        "batchId", inspection.batchId(),
        "inspectorId", inspection.inspectorId(),
        "inspectionType", inspection.inspectionType(),
        "standardVersion", inspection.standardVersion(),
        "resultStatus", inspection.resultStatus(),
        "previousBatchStatus", inspection.previousBatchStatus(),
        "receivedBatchQuantity", inspection.receivedBatchQuantity(),
        "inspectedAt", Formatters.instant(inspection.inspectedAt()),
        "items", items
    );
  }
}
