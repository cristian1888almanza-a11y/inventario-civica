package com.example.taskmanager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "asset_areas", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "code"}))
public class AssetArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private AssetSite site;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    public Long getId() { return id; }
    public AssetSite getSite() { return site; }
    public void setSite(AssetSite site) { this.site = site; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
