package com.example.taskmanager.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanager.dto.AssetDepreciationValuationResponse;
import com.example.taskmanager.dto.AssetAreaRequest;
import com.example.taskmanager.dto.AssetBatchRequest;
import com.example.taskmanager.dto.AssetOptionsResponse;
import com.example.taskmanager.dto.AssetRequest;
import com.example.taskmanager.dto.AssetResponse;
import com.example.taskmanager.dto.AssetStatusRequest;
import com.example.taskmanager.dto.AssetSiteRequest;
import com.example.taskmanager.dto.DepreciationBatchRequest;
import com.example.taskmanager.dto.DepreciationCalculateRequest;
import com.example.taskmanager.dto.DepreciationRecordResponse;
import com.example.taskmanager.dto.DepreciationReportResponse;
import com.example.taskmanager.report.ExcelReportWriter;
import com.example.taskmanager.service.AssetService;
import com.example.taskmanager.service.DepreciationService;

import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/assets")
public class AssetController {
    private final AssetService assetService;
    private final DepreciationService depreciationService;

    public AssetController(AssetService assetService, DepreciationService depreciationService) {
        this.assetService = assetService;
        this.depreciationService = depreciationService;
    }

    @GetMapping
    public List<AssetResponse> getAll() {
        return assetService.findAll();
    }

    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel() {
        List<AssetDepreciationValuationResponse> assets = depreciationService.getValuation(null);
        byte[] report = ExcelReportWriter.write(
            List.of("Código", "Activo", "Categoría", "Fecha de compra", "Valor de compra", "Vida útil (años)",
                "Depreciación mensual", "Depreciación acumulada", "Valor en libros", "Estado", "Fecha de corte"),
            assets.stream()
                .map(asset -> Arrays.<Object>asList(
                    asset.inventoryCode(), asset.name(), asset.categoryName(), asset.purchaseDate(),
                    asset.purchaseValue(), asset.usefulLifeYears(), asset.monthlyDepreciation(),
                    asset.accumulatedDepreciation(), asset.bookValue(), asset.assetStatus(), asset.valuationDate()))
                .toList()
        );
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-depreciacion-activos.xlsx").build().toString())
            .body(report);
    }

    @GetMapping("/depreciation/valuation")
    public List<AssetDepreciationValuationResponse> getDepreciationValuation(
        @RequestParam(required = false) LocalDate asOf
    ) {
        return depreciationService.getValuation(asOf);
    }

    @GetMapping("/options")
    public AssetOptionsResponse getOptions() {
        return assetService.options();
    }

    @PostMapping("/sites")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'CONFIG_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetOptionsResponse.SiteOption createSite(@Valid @RequestBody AssetSiteRequest request) {
        return assetService.createSite(request);
    }

    @PostMapping("/sites/{siteId}/areas")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'CONFIG_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetOptionsResponse.AreaOption createArea(@PathVariable Long siteId, @Valid @RequestBody AssetAreaRequest request) {
        return assetService.createArea(siteId, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(@Valid @RequestBody AssetRequest request) {
        return assetService.create(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssetResponse> createBatch(@Valid @RequestBody AssetBatchRequest request) {
        return assetService.createBatch(request);
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return assetService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public AssetResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AssetStatusRequest request) {
        return assetService.updateStatus(id, request);
    }

    @PostMapping("/{id}/depreciation/calculate")
    public DepreciationRecordResponse calculateDepreciation(@PathVariable Long id, @Valid @RequestBody DepreciationCalculateRequest request) {
        return depreciationService.calculateDepreciation(new DepreciationCalculateRequest(id, request.depreciationDate(), request.processedBy()));
    }

    @GetMapping("/{id}/depreciation/history")
    public List<DepreciationRecordResponse> getDepreciationHistory(@PathVariable Long id) {
        return depreciationService.getAssetHistory(id);
    }

    @PostMapping("/depreciation/batch")
    public DepreciationReportResponse depreciateAll(@Valid @RequestBody DepreciationBatchRequest request) {
        return depreciationService.depreciateAll(request);
    }

    @GetMapping("/depreciation/report/{year}/{month}")
    public DepreciationReportResponse getDepreciationReport(@PathVariable Integer year, @PathVariable Integer month) {
        return depreciationService.getPeriodReport(year, month);
    }
}
