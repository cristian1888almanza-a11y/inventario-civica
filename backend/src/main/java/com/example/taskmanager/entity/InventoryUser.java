package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "inventory_users", indexes = {
    @Index(name = "idx_inventory_users_enabled_name", columnList = "enabled, full_name")
}, uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class InventoryUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inventory_user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", nullable = false, length = 60)
    @Enumerated(EnumType.STRING)
    private Set<PermissionCode> permissions = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<PermissionCode> getPermissions() { return permissions; }
    public void setPermissions(Set<PermissionCode> permissions) { this.permissions = permissions == null ? new HashSet<>() : permissions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
