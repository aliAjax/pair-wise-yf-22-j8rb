package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.constants.UserRole;
import com.generated.qualityTrace.middlewares.AuthMiddleware;
import com.generated.qualityTrace.middlewares.RequireRoles;
import com.generated.qualityTrace.services.ProductBatchService;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.ProductBatchPayload;
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
@RequestMapping(RoutePaths.BATCHES)
public class ProductBatchController {
  private final ProductBatchService productBatchService;

  public ProductBatchController(ProductBatchService productBatchService) {
    this.productBatchService = productBatchService;
  }

  @GetMapping
  public List<Map<String, Object>> list() {
    return productBatchService.list();
  }

  @GetMapping("/{batchNo}")
  public Map<String, Object> get(@PathVariable String batchNo) {
    return productBatchService.getByBatchNo(batchNo);
  }

  @GetMapping("/{batchNo}/events")
  public List<Map<String, Object>> events(@PathVariable String batchNo) {
    return productBatchService.events(batchNo);
  }

  @PostMapping
  @RequireRoles({UserRole.LINE_SUPERVISOR})
  public Map<String, Object> create(@Valid @RequestBody ProductBatchPayload payload,
                                    HttpServletRequest request) {
    return productBatchService.create(payload, actor(request), request.getRequestURI());
  }

  private ActorContext actor(HttpServletRequest request) {
    return (ActorContext) request.getAttribute(AuthMiddleware.ACTOR_ATTRIBUTE);
  }
}
