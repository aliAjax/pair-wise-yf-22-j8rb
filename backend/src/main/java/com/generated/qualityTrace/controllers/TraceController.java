package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.services.TraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    /** 按批次号查询完整链路：工单开工 -> 批次投料 -> 检验 -> 不良处置。 */
    @GetMapping("/{batchNo}")
    public Map<String, Object> trace(@PathVariable String batchNo) {
        return traceService.trace(batchNo);
    }
}
