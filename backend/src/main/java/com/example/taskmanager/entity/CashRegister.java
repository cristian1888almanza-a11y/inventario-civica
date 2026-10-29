package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_registers", indexes = @Index(name = "idx_cash_status", columnList = "status"))
public class CashRegister {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 12) private String status;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal openingAmount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal balance;
    @Column(nullable = false, updatable = false) private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    @Column(length = 80) private String openedBy;
    @Column(length = 80) private String closedBy;
    @Column(precision = 14, scale = 2) private BigDecimal closingAmount;
    @PrePersist void created() { openedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getOpeningAmount() { return openingAmount; }
    public void setOpeningAmount(BigDecimal amount) { openingAmount = amount; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
    public BigDecimal getClosingAmount() { return closingAmount; }
    public void setClosingAmount(BigDecimal closingAmount) { this.closingAmount = closingAmount; }
}
