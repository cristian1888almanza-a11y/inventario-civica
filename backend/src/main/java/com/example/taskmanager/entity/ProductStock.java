package com.example.taskmanager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_stock", uniqueConstraints = @UniqueConstraint(
    name = "uk_product_stock_warehouse_product", columnNames = {"product_id", "warehouse_id"}))
public class ProductStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public int getAvailableQuantity() { return quantity - reservedQuantity; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
