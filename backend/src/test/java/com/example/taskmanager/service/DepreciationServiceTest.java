package com.example.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.taskmanager.dto.DepreciationCalculateRequest;
import com.example.taskmanager.entity.Asset;
import com.example.taskmanager.entity.AssetCategory;
import com.example.taskmanager.entity.AssetStatus;
import com.example.taskmanager.entity.DepreciationRecord;
import com.example.taskmanager.repository.AssetRepository;
import com.example.taskmanager.repository.DepreciationRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepreciationServiceTest {
    @Mock private AssetRepository assetRepository;
    @Mock private DepreciationRecordRepository depreciationRepository;
    @InjectMocks private DepreciationService service;

    @Test
    void calculatesThirtyElapsedMonthsUsingTwoDecimalRounding() {
        Asset asset = asset(7_000_000, 0, LocalDate.of(2024, 4, 1));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(depreciationRepository.findByAssetAndPeriodYearAndPeriodMonth(asset, 2026, 10)).thenReturn(List.of());
        when(depreciationRepository.save(any(DepreciationRecord.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.calculateDepreciation(new DepreciationCalculateRequest(
            1L, LocalDate.of(2026, 10, 6), "Prueba"));

        assertEquals(new BigDecimal("116666.67"), result.monthlyDepreciation());
        assertEquals(new BigDecimal("3500000.00"), result.accumulatedDepreciation());
        assertEquals(new BigDecimal("3500000.00"), result.bookValue());
    }

    @Test
    void doesNotDepreciateBelowResidualValue() {
        Asset asset = asset(7_000_000, 500_000, LocalDate.of(2020, 1, 1));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(depreciationRepository.findByAssetAndPeriodYearAndPeriodMonth(asset, 2025, 1)).thenReturn(List.of());
        when(depreciationRepository.save(any(DepreciationRecord.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.calculateDepreciation(new DepreciationCalculateRequest(
            1L, LocalDate.of(2025, 1, 1), "Prueba"));

        assertEquals(new BigDecimal("6500000.00"), result.accumulatedDepreciation());
        assertEquals(new BigDecimal("500000.00"), result.bookValue());
        assertEquals(AssetStatus.FULLY_DEPRECIATED, asset.getAssetStatus());
    }

    @Test
    void returnsExistingPeriodRecordWithoutSavingADuplicate() {
        Asset asset = asset(7_000_000, 0, LocalDate.of(2024, 4, 1));
        DepreciationRecord existing = new DepreciationRecord();
        existing.setAsset(asset);
        existing.setDepreciationDate(LocalDate.of(2026, 10, 6));
        existing.setPeriodYear(2026);
        existing.setPeriodMonth(10);
        existing.setMonthlyDepreciation(new BigDecimal("116666.67"));
        existing.setAccumulatedDepreciation(new BigDecimal("3500000.00"));
        existing.setBookValue(new BigDecimal("3500000.00"));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(depreciationRepository.findByAssetAndPeriodYearAndPeriodMonth(asset, 2026, 10))
            .thenReturn(List.of(existing));

        var result = service.calculateDepreciation(new DepreciationCalculateRequest(
            1L, LocalDate.of(2026, 10, 6), "Prueba"));

        assertEquals(new BigDecimal("3500000.00"), result.accumulatedDepreciation());
        verify(depreciationRepository, never()).save(any(DepreciationRecord.class));
    }

    private Asset asset(long purchaseValue, long residualValue, LocalDate startUseDate) {
        AssetCategory category = new AssetCategory();
        category.setName("Equipos de cómputo");
        category.setUsefulLifeYears(5);
        category.setAnnualDepreciationRate(new BigDecimal("0.20"));

        Asset asset = new Asset();
        asset.setInventoryCode("BIEN-000001");
        asset.setName("Portátil de prueba");
        asset.setAssetType("Equipo de cómputo");
        asset.setCategory(category);
        asset.setPurchaseValue(BigDecimal.valueOf(purchaseValue));
        asset.setResidualValue(BigDecimal.valueOf(residualValue));
        asset.setUsefulLifeYears(5);
        asset.setStartUseDate(startUseDate);
        asset.setAssetStatus(AssetStatus.ACTIVE);
        return asset;
    }
}