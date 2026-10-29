package com.example.taskmanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.taskmanager.entity.AssetCategory;

public record AssetCategoryResponse(
    Long id,
    String name,
    Integer usefulLifeYears,
    BigDecimal annualDepreciationRate,
    String depreciationMethod,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AssetCategoryResponse from(AssetCategory category) {
        return new AssetCategoryResponse(
            category.getId(),
            category.getName(),
            category.getUsefulLifeYears(),
            category.getAnnualDepreciationRate(),
            category.getDepreciationMethod(),
            category.getDescription(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}