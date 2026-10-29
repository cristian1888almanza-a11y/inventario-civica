package com.example.taskmanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 30, message = "El SKU no puede superar 30 caracteres")
    String sku,
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String name,
    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 80, message = "La categoría no puede superar 80 caracteres")
    String category,
    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    String description,
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    BigDecimal price,
    @Min(value = 0, message = "El stock no puede ser negativo")
    int stock,
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    int minStock,
    @Size(max = 120, message = "El proveedor no puede superar 120 caracteres")
    String supplier
) {}
