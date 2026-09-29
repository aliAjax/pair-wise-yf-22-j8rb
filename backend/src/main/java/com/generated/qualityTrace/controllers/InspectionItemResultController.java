package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.services.InspectionItemResultService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutePaths.INSPECTIONS)
public class InspectionItemResultController {
  private final InspectionItemResultService inspectionItemResultService;

  public InspectionItemResultController(InspectionItemResultService inspectionItemResultService) {
    this.inspectionItemResultService = inspectionItemResultService;
  }

  @GetMapping("/{inspectionId}/items")
  public List<Map<String, Object>> items(@PathVariable Long inspectionId) {
    return inspectionItemResultService.listByInspection(inspectionId);
  }
}
