package com.example.taskmanager.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssetCategoryRequest(
    @NotBlank @Size(max = 80) String name,
    @NotNull @Positive Integer usefulLifeYears,
    @NotNull @DecimalMin("0.0001") @DecimalMax("1.00") BigDecimal annualDepreciationRate,
    @Size(max = 30) String depreciationMethod,
    @Size(max = 500) String description
) {}