package com.example.taskmanager.dto;

import com.example.taskmanager.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BusinessResponses {
    private BusinessResponses() {}
    public record Line(Long productId, String sku, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record Operation(Long id, OperationType type, OperationStatus status, String customer, String description,
                            BigDecimal total, BigDecimal paidTotal, String receiptNumber, LocalDateTime createdAt,
                            List<Line> items) {
        public static Operation from(OperationRecord record) {
            return new Operation(record.getId(), record.getType(), record.getStatus(), record.getCustomer(),
                record.getDescription(), record.getTotal(), record.getPaidTotal(), record.getReceiptNumber(),
                record.getCreatedAt(), record.getLines().stream().map(line -> new Line(line.getProduct().getId(),
                    line.getProductSku(), line.getProductName(), line.getQuantity(), line.getUnitPrice(),
                    line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))).toList());
        }
    }
    public record Cash(Long id, String status, BigDecimal openingAmount, BigDecimal balance, BigDecimal closingAmount,
                       LocalDateTime openedAt, LocalDateTime closedAt) {
        public static Cash from(CashRegister register) {
            return new Cash(register.getId(), register.getStatus(), register.getOpeningAmount(), register.getBalance(),
                register.getClosingAmount(), register.getOpenedAt(), register.getClosedAt());
        }
    }
    public record CashMovement(Long id, String direction, BigDecimal amount, String description, String createdBy,
                               LocalDateTime createdAt) {
        public static CashMovement from(com.example.taskmanager.entity.CashMovement movement) {
            return new CashMovement(movement.getId(), movement.getDirection(), movement.getAmount(),
                movement.getDescription(), movement.getCreatedBy(), movement.getCreatedAt());
        }
    }
}
