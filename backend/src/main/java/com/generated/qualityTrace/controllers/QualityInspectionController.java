package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.middlewares.CurrentUser;
import com.generated.qualityTrace.services.QualityInspectionService;
import com.generated.qualityTrace.types.QualityInspectionPayload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inspections")
public class QualityInspectionController {

    private final QualityInspectionService inspectionService;

    public QualityInspectionController(QualityInspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    /**
     * 提交检验（逐项写入）。同批次并发回传由服务层行锁串行接续；
     * 中途任何失败整批回滚。重复提交可带 Idempotency-Key 直接回放首条结果。
     */
    @PostMapping
    public Map<String, Object> submit(
            @Valid @RequestBody QualityInspectionPayload payload,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        CurrentUser.Principal principal = CurrentUser.get();
        return inspectionService.submit(payload, principal.userId(), idempotencyKey);
    }
}
