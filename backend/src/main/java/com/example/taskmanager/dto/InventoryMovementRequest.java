package com.example.taskmanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InventoryMovementRequest(@NotNull @Positive Long productId,
                                       @NotBlank String type,
                                       @Min(0) int quantity,
                                       @NotBlank @Size(max = 250) String reason) {
}
