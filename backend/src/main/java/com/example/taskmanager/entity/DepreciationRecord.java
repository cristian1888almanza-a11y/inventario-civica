package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depreciation_records", indexes = {
    @Index(name = "idx_depreciation_asset_date", columnList = "asset_id, depreciation_date"),
    @Index(name = "idx_depreciation_period", columnList = "period_year, period_month")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_depreciation_asset_period", columnNames = {"asset_id", "period_year", "period_month"})
})
public class DepreciationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "depreciation_date", nullable = false)
    private LocalDate depreciationDate;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal monthlyDepreciation;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal accumulatedDepreciation;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal bookValue;

    @Column(name = "processed_by", length = 120)
    private String processedBy;

    @Column(name = "is_batch_process", nullable = false)
    private Boolean isBatchProcess = false;

    @Column(name = "batch_reference", length = 60)
    private String batchReference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public LocalDate getDepreciationDate() { return depreciationDate; }
    public void setDepreciationDate(LocalDate depreciationDate) { this.depreciationDate = depreciationDate; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public BigDecimal getMonthlyDepreciation() { return monthlyDepreciation; }
    public void setMonthlyDepreciation(BigDecimal monthlyDepreciation) { this.monthlyDepreciation = monthlyDepreciation; }
    public BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) { this.accumulatedDepreciation = accumulatedDepreciation; }
    public BigDecimal getBookValue() { return bookValue; }
    public void setBookValue(BigDecimal bookValue) { this.bookValue = bookValue; }
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    public Boolean getIsBatchProcess() { return isBatchProcess; }
    public void setIsBatchProcess(Boolean isBatchProcess) { this.isBatchProcess = isBatchProcess; }
    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
