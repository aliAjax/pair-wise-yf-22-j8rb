package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.constants.UserRole;
import com.generated.qualityTrace.middlewares.AuthMiddleware;
import com.generated.qualityTrace.middlewares.RequireRoles;
import com.generated.qualityTrace.services.DefectRecordService;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.CloseDefectPayload;
import com.generated.qualityTrace.types.DefectDispositionPayload;
import com.generated.qualityTrace.types.DefectRecordPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutePaths.DEFECTS)
public class DefectRecordController {
  private final DefectRecordService defectRecordService;

  public DefectRecordController(DefectRecordService defectRecordService) {
    this.defectRecordService = defectRecordService;
  }

  @PostMapping
  @RequireRoles({UserRole.QUALITY_INSPECTOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> register(@Valid @RequestBody DefectRecordPayload payload,
                                      HttpServletRequest request) {
    return defectRecordService.register(payload, actor(request), request.getRequestURI());
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable Long id) {
    return defectRecordService.get(id);
  }

  @GetMapping("/batch/{batchId}")
  public List<Map<String, Object>> byBatch(@PathVariable Long batchId) {
    return defectRecordService.listByBatchId(batchId);
  }

  @GetMapping("/{id}/events")
  public List<Map<String, Object>> events(@PathVariable Long id) {
    return defectRecordService.events(id);
  }

  @PostMapping("/{id}/disposition")
  @RequireRoles({UserRole.QUALITY_MANAGER})
  public Map<String, Object> dispose(@PathVariable Long id,
                                     @Valid @RequestBody DefectDispositionPayload payload,
                                     HttpServletRequest request) {
    return defectRecordService.dispose(id, payload, actor(request), request.getRequestURI());
  }

  @PostMapping("/{id}/close")
  @RequireRoles({UserRole.QUALITY_MANAGER})
  public Map<String, Object> close(@PathVariable Long id,
                                   @Valid @RequestBody CloseDefectPayload payload,
                                   HttpServletRequest request) {
    return defectRecordService.close(id, payload, actor(request), request.getRequestURI());
  }

  private ActorContext actor(HttpServletRequest request) {
    return (ActorContext) request.getAttribute(AuthMiddleware.ACTOR_ATTRIBUTE);
  }
}
