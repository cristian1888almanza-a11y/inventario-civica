package com.example.taskmanager.dto;

import com.example.taskmanager.entity.ProductStock;

public record ProductStockResponse(Long productId, String sku, String productName, Long warehouseId,
                                   String warehouseCode, int quantity, int reservedQuantity,
                                   int availableQuantity) {
    public static ProductStockResponse from(ProductStock stock) {
        return new ProductStockResponse(stock.getProduct().getId(), stock.getProduct().getSku(),
            stock.getProduct().getName(), stock.getWarehouse().getId(), stock.getWarehouse().getCode(),
            stock.getQuantity(), stock.getReservedQuantity(), stock.getAvailableQuantity());
    }
}
