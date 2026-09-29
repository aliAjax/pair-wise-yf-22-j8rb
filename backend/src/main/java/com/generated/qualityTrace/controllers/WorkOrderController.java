package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.RoutePaths;
import com.generated.qualityTrace.constants.UserRole;
import com.generated.qualityTrace.constants.WorkOrderStatus;
import com.generated.qualityTrace.middlewares.AuthMiddleware;
import com.generated.qualityTrace.middlewares.RequireRoles;
import com.generated.qualityTrace.services.WorkOrderService;
import com.generated.qualityTrace.types.ActorContext;
import com.generated.qualityTrace.types.WorkOrderActionPayload;
import com.generated.qualityTrace.types.WorkOrderPayload;
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
@RequestMapping(RoutePaths.WORK_ORDERS)
public class WorkOrderController {
  private final WorkOrderService workOrderService;

  public WorkOrderController(WorkOrderService workOrderService) {
    this.workOrderService = workOrderService;
  }

  @GetMapping
  public List<Map<String, Object>> list() {
    return workOrderService.list();
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable Long id) {
    return workOrderService.get(id);
  }

  @GetMapping("/order/{orderNo}")
  public Map<String, Object> getByOrderNo(@PathVariable String orderNo) {
    return workOrderService.getByOrderNo(orderNo);
  }

  @GetMapping("/{id}/events")
  public List<Map<String, Object>> events(@PathVariable Long id) {
    return workOrderService.events(id);
  }

  @PostMapping
  @RequireRoles({UserRole.LINE_SUPERVISOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> create(@Valid @RequestBody WorkOrderPayload payload,
                                    HttpServletRequest request) {
    return workOrderService.create(payload, actor(request), request.getRequestURI());
  }

  @PostMapping("/{id}/start")
  @RequireRoles({UserRole.LINE_SUPERVISOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> start(@PathVariable Long id,
                                   @RequestBody(required = false) WorkOrderActionPayload payload,
                                   HttpServletRequest request) {
    return workOrderService.changeStatus(id, WorkOrderStatus.RUNNING, payload, actor(request),
        request.getRequestURI(), request.getMethod());
  }

  @PostMapping("/{id}/pause")
  @RequireRoles({UserRole.LINE_SUPERVISOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> pause(@PathVariable Long id,
                                   @RequestBody(required = false) WorkOrderActionPayload payload,
                                   HttpServletRequest request) {
    return workOrderService.changeStatus(id, WorkOrderStatus.PAUSED, payload, actor(request),
        request.getRequestURI(), request.getMethod());
  }

  @PostMapping("/{id}/finish")
  @RequireRoles({UserRole.LINE_SUPERVISOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> finish(@PathVariable Long id,
                                    @RequestBody(required = false) WorkOrderActionPayload payload,
                                    HttpServletRequest request) {
    return workOrderService.changeStatus(id, WorkOrderStatus.FINISHED, payload, actor(request),
        request.getRequestURI(), request.getMethod());
  }

  @PostMapping("/{id}/cancel")
  @RequireRoles({UserRole.LINE_SUPERVISOR, UserRole.QUALITY_MANAGER})
  public Map<String, Object> cancel(@PathVariable Long id,
                                    @RequestBody(required = false) WorkOrderActionPayload payload,
                                    HttpServletRequest request) {
    return workOrderService.changeStatus(id, WorkOrderStatus.CANCELLED, payload, actor(request),
        request.getRequestURI(), request.getMethod());
  }

  private ActorContext actor(HttpServletRequest request) {
    return (ActorContext) request.getAttribute(AuthMiddleware.ACTOR_ATTRIBUTE);
  }
}
