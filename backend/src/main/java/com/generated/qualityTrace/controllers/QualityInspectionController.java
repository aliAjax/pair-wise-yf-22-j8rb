package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.constants.UserRole;
import com.generated.qualityTrace.middlewares.AuthMiddleware;
import com.generated.qualityTrace.middlewares.RequireRoles;
import com.generated.qualityTrace.services.QualityInspectionService;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.QualityInspectionPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutePaths.INSPECTIONS)
public class QualityInspectionController {
  private final QualityInspectionService qualityInspectionService;

  public QualityInspectionController(QualityInspectionService qualityInspectionService) {
    this.qualityInspectionService = qualityInspectionService;
  }

  @PostMapping
  @RequireRoles({UserRole.QUALITY_INSPECTOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> submit(@Valid @RequestBody QualityInspectionPayload payload,
                                    HttpServletRequest request) {
    return qualityInspectionService.submit(payload, actor(request), request.getRequestURI());
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable Long id) {
    return qualityInspectionService.get(id);
  }

  @GetMapping("/batch/{batchId}")
  public java.util.List<Map<String, Object>> byBatch(@PathVariable Long batchId) {
    return qualityInspectionService.listByBatchId(batchId);
  }

  private ActorContext actor(HttpServletRequest request) {
    return (ActorContext) request.getAttribute(AuthMiddleware.ACTOR_ATTRIBUTE);
  }
}
