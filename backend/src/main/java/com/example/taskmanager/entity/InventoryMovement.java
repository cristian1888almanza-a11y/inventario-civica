package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements", indexes = {
    @Index(name = "idx_inventory_movement_created", columnList = "created_at"),
    @Index(name = "idx_inventory_movement_product", columnList = "product_id")
})
public class InventoryMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "previous_quantity")
    private Integer previousQuantity;

    @Column(name = "resulting_quantity")
    private Integer resultingQuantity;

    @Column(nullable = false, length = 250)
    private String reason;

    @Column(nullable = false, length = 50)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    public Integer getResultingQuantity() { return resultingQuantity; }
    public void setResultingQuantity(Integer resultingQuantity) { this.resultingQuantity = resultingQuantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
