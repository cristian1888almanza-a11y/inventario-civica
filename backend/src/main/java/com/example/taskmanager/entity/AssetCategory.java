package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_categories", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class AssetCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer usefulLifeYears;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal annualDepreciationRate;

    @Column(nullable = false, length = 30)
    private String depreciationMethod = "STRAIGHT_LINE";

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getUsefulLifeYears() { return usefulLifeYears; }
    public void setUsefulLifeYears(Integer usefulLifeYears) { this.usefulLifeYears = usefulLifeYears; }
    public BigDecimal getAnnualDepreciationRate() { return annualDepreciationRate; }
    public void setAnnualDepreciationRate(BigDecimal annualDepreciationRate) { this.annualDepreciationRate = annualDepreciationRate; }
    public String getDepreciationMethod() { return depreciationMethod; }
    public void setDepreciationMethod(String depreciationMethod) { this.depreciationMethod = depreciationMethod; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}