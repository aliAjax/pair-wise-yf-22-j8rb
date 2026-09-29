package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.services.WorkOrderService;
import com.generated.qualityTrace.types.WorkOrderPayload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody WorkOrderPayload payload) {
        return workOrderService.create(payload.getOrderNo(), payload.getProductCode(),
                payload.getProductName(), payload.getPlannedQty(), payload.getLineCode());
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return workOrderService.list();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return workOrderService.detail(id);
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> start(@PathVariable Long id) {
        CurrentUser.username(); // 认证主体由 AuthMiddleware 保证存在
        return workOrderService.start(id);
    }

    @PostMapping("/{id}/pause")
    public Map<String, Object> pause(@PathVariable Long id) {
        return workOrderService.pause(id);
    }

    @PostMapping("/{id}/finish")
    public Map<String, Object> finish(@PathVariable Long id) {
        return workOrderService.finish(id);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable Long id) {
        return workOrderService.cancel(id);
    }
}
