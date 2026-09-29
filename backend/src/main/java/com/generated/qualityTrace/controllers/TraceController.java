package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.constants.UserRole;
import com.generated.qualityTrace.middlewares.RequireRoles;
import com.generated.qualityTrace.services.TraceService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutePaths.TRACE)
public class TraceController {
  private final TraceService traceService;

  public TraceController(TraceService traceService) {
    this.traceService = traceService;
  }

  @GetMapping("/{batchNo}")
  @RequireRoles({
      UserRole.QUALITY_INSPECTOR,
      UserRole.LINE_SUPERVISOR,
      UserRole.QUALITY_MANAGER,
      UserRole.AUDITOR
  })
  public Map<String, Object> trace(@PathVariable String batchNo) {
    return traceService.traceByBatchNo(batchNo);
  }
}
