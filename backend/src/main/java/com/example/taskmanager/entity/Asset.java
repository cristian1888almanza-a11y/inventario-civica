package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets", indexes = {
    @Index(name = "idx_assets_site_area", columnList = "site_id, area_id"),
    @Index(name = "idx_assets_responsible_status", columnList = "responsible_user_id, asset_status"),
    @Index(name = "idx_assets_created_at", columnList = "created_at")
}, uniqueConstraints = @UniqueConstraint(columnNames = "inventory_code"))
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_code", nullable = false, unique = true, length = 40)
    private String inventoryCode;

    @Column(nullable = false, length = 80)
    private String assetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String serialNumber;

    @Column(length = 80)
    private String brand;

    @Column(length = 80)
    private String model;

    @Column(length = 120)
    private String material;

    @Column(length = 80)
    private String color;

    @Column(length = 120)
    private String dimensions;

    @Column(length = 160)
    private String itemDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private BusinessSettingsRecord company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private AssetSite site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private AssetArea areaRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private InventoryUser responsibleUser;

    @Column(nullable = false, length = 160)
    private String location;

    @Column(nullable = false, length = 100)
    private String area;

    @Column(nullable = false, length = 40)
    private String condition;

    @Column(length = 120)
    private String responsible;

    @Column(length = 120)
    private String processor;

    @Column(length = 80)
    private String ram;

    @Column(length = 120)
    private String storage;

    @Column(length = 120)
    private String operatingSystem;

    private LocalDate purchaseDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal purchaseValue;

    @Column(length = 3)
    private String currency;

    private LocalDate warrantyUntil;

    @Column(length = 120)
    private String vendor;

    @Column(length = 120)
    private String purchaseReference;

    @Column(length = 1500)
    private String observations;

    private LocalDate startUseDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal residualValue;

    private Integer usefulLifeYears;

    @Column(precision = 14, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(precision = 14, scale = 2)
    private BigDecimal bookValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssetStatus assetStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (assetStatus == null) {
            assetStatus = AssetStatus.ACTIVE;
        }
        if (accumulatedDepreciation == null) {
            accumulatedDepreciation = BigDecimal.ZERO;
        }
        if (bookValue == null && purchaseValue != null) {
            bookValue = purchaseValue;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getInventoryCode() { return inventoryCode; }
    public void setInventoryCode(String inventoryCode) { this.inventoryCode = inventoryCode; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getItemDetails() { return itemDetails; }
    public void setItemDetails(String itemDetails) { this.itemDetails = itemDetails; }
    public BusinessSettingsRecord getCompany() { return company; }
    public void setCompany(BusinessSettingsRecord company) { this.company = company; }
    public AssetSite getSite() { return site; }
    public void setSite(AssetSite site) { this.site = site; }
    public AssetArea getAreaRecord() { return areaRecord; }
    public void setAreaRecord(AssetArea areaRecord) { this.areaRecord = areaRecord; }
    public InventoryUser getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(InventoryUser responsibleUser) { this.responsibleUser = responsibleUser; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }
    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPurchaseValue() { return purchaseValue; }
    public void setPurchaseValue(BigDecimal purchaseValue) { this.purchaseValue = purchaseValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getWarrantyUntil() { return warrantyUntil; }
    public void setWarrantyUntil(LocalDate warrantyUntil) { this.warrantyUntil = warrantyUntil; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getPurchaseReference() { return purchaseReference; }
    public void setPurchaseReference(String purchaseReference) { this.purchaseReference = purchaseReference; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public LocalDate getStartUseDate() { return startUseDate; }
    public void setStartUseDate(LocalDate startUseDate) { this.startUseDate = startUseDate; }
    public BigDecimal getResidualValue() { return residualValue; }
    public void setResidualValue(BigDecimal residualValue) { this.residualValue = residualValue; }
    public Integer getUsefulLifeYears() { return usefulLifeYears; }
    public void setUsefulLifeYears(Integer usefulLifeYears) { this.usefulLifeYears = usefulLifeYears; }
    public BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) { this.accumulatedDepreciation = accumulatedDepreciation; }
    public BigDecimal getBookValue() { return bookValue; }
    public void setBookValue(BigDecimal bookValue) { this.bookValue = bookValue; }
    public AssetStatus getAssetStatus() { return assetStatus; }
    public void setAssetStatus(AssetStatus assetStatus) { this.assetStatus = assetStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
