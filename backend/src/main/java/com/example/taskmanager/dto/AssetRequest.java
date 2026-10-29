package com.example.taskmanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetRequest(
    @NotBlank @Size(max = 80) String assetType,
    @NotNull @Positive Long categoryId,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 120) String serialNumber,
    @Size(max = 80) String brand,
    @Size(max = 80) String model,
    @Size(max = 120) String material,
    @Size(max = 80) String color,
    @Size(max = 120) String dimensions,
    @Size(max = 160) String itemDetails,
    @Positive Long companyId,
    @Positive Long siteId,
    @Positive Long areaId,
    @Positive Long responsibleId,
    @NotBlank @Size(max = 160) String location,
    @NotBlank @Size(max = 100) String area,
    @NotBlank @Size(max = 40) String condition,
    @Size(max = 120) String responsible,
    @Size(max = 120) String processor,
    @Size(max = 80) String ram,
    @Size(max = 120) String storage,
    @Size(max = 120) String operatingSystem,
    LocalDate purchaseDate,
    @DecimalMin("0.01") BigDecimal purchaseValue,
    @Size(max = 3) String currency,
    LocalDate warrantyUntil,
    @Size(max = 120) String vendor,
    @Size(max = 120) String purchaseReference,
    @Size(max = 1500) String observations,
    LocalDate startUseDate,
    @NotNull @DecimalMin("0.00") BigDecimal residualValue,
    @Positive Integer usefulLifeYears
) {}
