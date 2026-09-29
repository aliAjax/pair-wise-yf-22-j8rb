package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.models.InspectionItemResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InspectionItemResultDtoFactory {
  private InspectionItemResultDtoFactory() {}

  public static Map<String, Object> response(InspectionItemResult item) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", item.id());
    dto.put("inspectionId", item.inspectionId());
    dto.put("itemCode", item.itemCode());
    dto.put("itemName", item.itemName());
    dto.put("measuredValue", item.measuredValue());
    dto.put("limitMin", item.limitMin());
    dto.put("limitMax", item.limitMax());
    dto.put("itemStatus", item.itemStatus());
    return dto;
  }
}
