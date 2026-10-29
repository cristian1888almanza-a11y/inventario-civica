package com.example.taskmanager.dto;

import java.math.BigDecimal;

public record InventorySummary(
    long totalProducts,
    long totalUnits,
    BigDecimal inventoryValue,
    long lowStock
) {}
