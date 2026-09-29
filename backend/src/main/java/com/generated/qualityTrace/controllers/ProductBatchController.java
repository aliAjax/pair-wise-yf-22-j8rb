package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.services.ProductBatchService;
import com.generated.qualityTrace.types.ProductBatchPayload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class ProductBatchController {

    private final ProductBatchService batchService;

    public ProductBatchController(ProductBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody ProductBatchPayload payload) {
        return batchService.create(payload.getBatchNo(), payload.getWorkOrderId(),
                payload.getQuantity(), payload.getMaterialLotNo());
    }

    @GetMapping("/{batchNo}")
    public Map<String, Object> detail(@PathVariable String batchNo) {
        return batchService.detailByBatchNo(batchNo);
    }
}
