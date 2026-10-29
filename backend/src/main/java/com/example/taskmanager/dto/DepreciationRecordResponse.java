package com.example.taskmanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.taskmanager.entity.DepreciationRecord;

public record DepreciationRecordResponse(
    Long id,
    Long assetId,
    String assetInventoryCode,
    String assetName,
    LocalDate depreciationDate,
    Integer periodYear,
    Integer periodMonth,
    BigDecimal monthlyDepreciation,
    BigDecimal accumulatedDepreciation,
    BigDecimal bookValue,
    String processedBy,
    Boolean isBatchProcess,
    String batchReference,
    LocalDateTime createdAt
) {
    public static DepreciationRecordResponse from(DepreciationRecord record) {
        return new DepreciationRecordResponse(
            record.getId(),
            record.getAsset().getId(),
            record.getAsset().getInventoryCode(),
            record.getAsset().getName(),
            record.getDepreciationDate(),
            record.getPeriodYear(),
            record.getPeriodMonth(),
            record.getMonthlyDepreciation(),
            record.getAccumulatedDepreciation(),
            record.getBookValue(),
            record.getProcessedBy(),
            record.getIsBatchProcess(),
            record.getBatchReference(),
            record.getCreatedAt()
        );
    }
}