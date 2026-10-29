package com.example.taskmanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DepreciationCalculateRequest(
    @NotNull @Positive Long assetId,
    @NotNull LocalDate depreciationDate,
    @Size(max = 120) String processedBy
) {}