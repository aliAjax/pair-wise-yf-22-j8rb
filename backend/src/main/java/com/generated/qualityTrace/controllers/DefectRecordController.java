package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.services.DefectRecordService;
import com.generated.qualityTrace.types.DefectDispositionPayload;
import com.generated.qualityTrace.types.DefectRecordPayload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/defects")
public class DefectRecordController {

    private final DefectRecordService defectService;

    public DefectRecordController(DefectRecordService defectService) {
        this.defectService = defectService;
    }

    @PostMapping
    public Map<String, Object> register(@Valid @RequestBody DefectRecordPayload payload) {
        return defectService.register(payload.getBatchNo(), payload.getDefectType(),
                payload.getDefectQty(), payload.getSeverity(), payload.getRootCause(),
                payload.getOriginalResult());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return defectService.detail(id);
    }

    /** 处置流转（处置中/返工/让步/报废）。 */
    @PostMapping("/{id}/disposition")
    public Map<String, Object> dispose(@PathVariable Long id,
                                       @Valid @RequestBody DefectDispositionPayload payload) {
        return defectService.dispose(id, payload.getTargetStatus(), payload.getDispositionNote());
    }

    /** 关闭不良处置：原判定与所有变更前后值仍保留可查。关闭结论必填。 */
    @PostMapping("/{id}/close")
    public Map<String, Object> close(@PathVariable Long id,
                                     @RequestBody(required = false) DefectDispositionPayload payload) {
        String note = payload == null ? null : payload.getDispositionNote();
        return defectService.dispose(id, "CLOSED", note);
    }
}
