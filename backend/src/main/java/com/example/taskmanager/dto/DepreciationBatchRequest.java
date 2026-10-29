package com.example.taskmanager.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepreciationBatchRequest(
    @NotNull LocalDate depreciationDate,
    @Size(max = 120) String processedBy
) {}