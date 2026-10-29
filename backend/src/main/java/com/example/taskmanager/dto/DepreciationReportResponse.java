package com.example.taskmanager.dto;

import java.math.BigDecimal;
import java.util.List;

public record DepreciationReportResponse(
    Integer periodYear,
    Integer periodMonth,
    Integer totalAssets,
    BigDecimal totalMonthlyDepreciation,
    BigDecimal totalAccumulatedDepreciation,
    BigDecimal totalBookValue,
    List<DepreciationRecordResponse> records
) {}