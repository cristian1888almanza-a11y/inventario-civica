package com.example.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_settings")
public class BusinessSettingsRecord {
    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 120)
    private String businessName = "La Cívica";

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    public Long getId() { return id; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
