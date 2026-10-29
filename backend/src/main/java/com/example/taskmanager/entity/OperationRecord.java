package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business_operations", indexes = {
    @Index(name = "idx_operation_type_created", columnList = "type,created_at"),
    @Index(name = "idx_operation_receipt", columnList = "receipt_number", unique = true)
})
public class OperationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OperationType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OperationStatus status;
    @Column(length = 160) private String customer;
    @Column(length = 500) private String description;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal paidTotal = BigDecimal.ZERO;
    @Column(length = 40) private String receiptNumber;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationLine> lines = new ArrayList<>();

    @PrePersist void createDates() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updateDate() { updatedAt = LocalDateTime.now(); }
    public void addLine(OperationLine line) { lines.add(line); line.setOperation(this); }
    public Long getId() { return id; }
    public OperationType getType() { return type; }
    public void setType(OperationType type) { this.type = type; }
    public OperationStatus getStatus() { return status; }
    public void setStatus(OperationStatus status) { this.status = status; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getPaidTotal() { return paidTotal; }
    public void setPaidTotal(BigDecimal paidTotal) { this.paidTotal = paidTotal; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OperationLine> getLines() { return lines; }
}
