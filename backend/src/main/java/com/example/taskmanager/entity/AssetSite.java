package com.example.taskmanager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "asset_sites", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
public class AssetSite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private BusinessSettingsRecord company;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String city;

    public Long getId() { return id; }
    public BusinessSettingsRecord getCompany() { return company; }
    public void setCompany(BusinessSettingsRecord company) { this.company = company; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
