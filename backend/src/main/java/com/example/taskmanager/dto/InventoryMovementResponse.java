package com.example.taskmanager.dto;

import com.example.taskmanager.entity.InventoryMovement;
import java.time.LocalDateTime;

public record InventoryMovementResponse(Long id, Long productId, String productName, String type,
                                        int quantity, Integer previousQuantity, Integer resultingQuantity,
                                        String reason, String createdBy, LocalDateTime createdAt) {
    public static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(movement.getId(), movement.getProductId(), movement.getProductName(),
            movement.getType(), movement.getQuantity(), movement.getPreviousQuantity(), movement.getResultingQuantity(),
            movement.getReason(), movement.getCreatedBy(), movement.getCreatedAt());
    }
}
