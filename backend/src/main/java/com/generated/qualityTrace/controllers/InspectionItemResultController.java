package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.services.InspectionItemResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspection-items")
public class InspectionItemResultController {

    private final InspectionItemResultService itemService;

    public InspectionItemResultController(InspectionItemResultService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/by-inspection/{inspectionId}")
    public List<Map<String, Object>> listByInspection(@PathVariable Long inspectionId) {
        return itemService.listByInspection(inspectionId);
    }
}
