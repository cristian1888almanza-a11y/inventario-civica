package com.example.taskmanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.taskmanager.entity.Asset;
import com.example.taskmanager.entity.AssetStatus;

public record AssetDepreciationValuationResponse(
    Long id,
    String inventoryCode,
    String name,
    String categoryName,
    LocalDate purchaseDate,
    LocalDate startUseDate,
    BigDecimal purchaseValue,
    Integer usefulLifeYears,
    BigDecimal annualDepreciationRate,
    BigDecimal monthlyDepreciation,
    BigDecimal accumulatedDepreciation,
    BigDecimal bookValue,
    String condition,
    String responsible,
    String location,
    String department,
    String city,
    AssetStatus assetStatus,
    LocalDate valuationDate
) {
    public static AssetDepreciationValuationResponse from(
        Asset asset,
        LocalDate valuationDate,
        BigDecimal annualDepreciationRate,
        BigDecimal monthlyDepreciation,
        BigDecimal accumulatedDepreciation,
        BigDecimal bookValue,
        AssetStatus assetStatus
    ) {
        return new AssetDepreciationValuationResponse(
            asset.getId(),
            asset.getInventoryCode(),
            asset.getName(),
            asset.getCategory() == null ? null : asset.getCategory().getName(),
            asset.getPurchaseDate(),
            asset.getStartUseDate(),
            asset.getPurchaseValue(),
            asset.getUsefulLifeYears(),
            annualDepreciationRate,
            monthlyDepreciation,
            accumulatedDepreciation,
            bookValue,
            asset.getCondition(),
            asset.getResponsible(),
            asset.getLocation(),
            asset.getSite() == null ? null : asset.getSite().getDepartment(),
            asset.getSite() == null ? null : asset.getSite().getCity(),
            assetStatus,
            valuationDate
        );
    }
}
