package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_movements", indexes = {
    @Index(name = "idx_cash_movements_register_created", columnList = "register_id, created_at")
})
public class CashMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "register_id", nullable = false) private CashRegister register;
    @Column(nullable = false, length = 10) private String direction;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 240) private String description;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(length = 80) private String createdBy;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
    public void setRegister(CashRegister register) { this.register = register; }
    public void setDirection(String direction) { this.direction = direction; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Long getId() { return id; }
    public String getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
