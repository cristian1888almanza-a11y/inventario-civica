package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "business_operation_lines")
public class OperationLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private OperationRecord operation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false, length = 30) private String productSku;
    @Column(nullable = false, length = 120) private String productName;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;

    public void setOperation(OperationRecord operation) { this.operation = operation; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
