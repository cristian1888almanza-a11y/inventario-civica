package com.example.taskmanager.controller;

import com.example.taskmanager.dto.DepreciationBatchRequest;
import com.example.taskmanager.dto.DepreciationCalculateRequest;
import com.example.taskmanager.dto.DepreciationRecordResponse;
import com.example.taskmanager.dto.DepreciationReportResponse;
import com.example.taskmanager.service.DepreciationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/depreciation")
public class DepreciationController {
    private final DepreciationService service;

    public DepreciationController(DepreciationService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'DEPRECIACION_WRITE')")
    public DepreciationRecordResponse calculate(@Valid @RequestBody DepreciationCalculateRequest request) {
        return service.calculateDepreciation(request);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'DEPRECIACION_WRITE')")
    public DepreciationReportResponse depreciateAll(@Valid @RequestBody DepreciationBatchRequest request) {
        return service.depreciateAll(request);
    }

    @GetMapping("/asset/{assetId}/history")
    public List<DepreciationRecordResponse> getAssetHistory(@PathVariable Long assetId) {
        return service.getAssetHistory(assetId);
    }

    @GetMapping("/report/{year}/{month}")
    public DepreciationReportResponse getPeriodReport(@PathVariable Integer year, @PathVariable Integer month) {
        return service.getPeriodReport(year, month);
    }
}
