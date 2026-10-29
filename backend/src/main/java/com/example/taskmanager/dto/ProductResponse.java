package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String category,
    String description,
    BigDecimal price,
    int stock,
    int minStock,
    String supplier,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(), product.getSku(), product.getName(), product.getCategory(),
            product.getDescription(), product.getPrice(), product.getStock(), product.getMinStock(),
            product.getSupplier(), product.getCreatedAt(), product.getUpdatedAt()
        );
    }
}
