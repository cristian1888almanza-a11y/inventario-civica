package com.example.taskmanager.service;

import com.example.taskmanager.dto.InventoryMovementRequest;
import com.example.taskmanager.dto.InventoryMovementResponse;
import com.example.taskmanager.entity.InventoryMovement;
import com.example.taskmanager.entity.Product;
import com.example.taskmanager.exception.ProductNotFoundException;
import com.example.taskmanager.repository.InventoryMovementRepository;
import com.example.taskmanager.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class InventoryMovementService {
    private final InventoryMovementRepository movements;
    private final ProductRepository products;
    private final ActivityLogService activityLog;
    private final WarehouseService warehouses;

    public InventoryMovementService(InventoryMovementRepository movements, ProductRepository products,
                                    ActivityLogService activityLog, WarehouseService warehouses) {
        this.movements = movements;
        this.products = products;
        this.activityLog = activityLog;
        this.warehouses = warehouses;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> recent() {
        return movements.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 300))
            .stream().map(InventoryMovementResponse::from).toList();
    }

    @Transactional
    public InventoryMovementResponse adjust(InventoryMovementRequest request) {
        String type = request.type().trim().toUpperCase();
        if (!Set.of("IN", "OUT", "ADJUSTMENT").contains(type)) {
            throw new IllegalArgumentException("Tipo de movimiento de inventario no válido");
        }
        if (!type.equals("ADJUSTMENT") && request.quantity() == 0) {
            throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor que cero");
        }
        Product product = products.findByIdForUpdate(request.productId())
            .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        int quantity = type.equals("ADJUSTMENT") ? request.quantity() - product.getStock() : request.quantity();
        if (type.equals("OUT") && quantity > product.getStock()) {
            throw new IllegalArgumentException("No hay existencias suficientes para registrar la salida");
        }
        product.setStock(type.equals("ADJUSTMENT") ? request.quantity()
            : product.getStock() + (type.equals("IN") ? quantity : -quantity));
        InventoryMovement movement = persistMovement(product, type, quantity, request.reason());
        activityLog.record("INVENTORY_MOVEMENT_" + type,
            request.reason().trim() + ": " + product.getName() + " (" + signed(quantity) + " unidades)");
        return InventoryMovementResponse.from(movement);
    }

    @Transactional
    public void record(Product product, String type, int quantity, String reason) {
        if (quantity == 0) return;
        persistMovement(product, type, quantity, reason);
    }

    private InventoryMovement persistMovement(Product product, String type, int quantity, String reason) {
        InventoryMovement movement = new InventoryMovement();
        movement.setProductId(product.getId());
        movement.setProductName(product.getName());
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setPreviousQuantity(product.getStock() - quantity);
        movement.setResultingQuantity(product.getStock());
        movement.setReason(reason.trim());
        movement.setCreatedBy(activityLog.currentUsername());
        warehouses.synchronizeDefaultStock(product);
        return movements.save(movement);
    }

    private String signed(int quantity) {
        return quantity > 0 ? "+" + quantity : Integer.toString(quantity);
    }
}
