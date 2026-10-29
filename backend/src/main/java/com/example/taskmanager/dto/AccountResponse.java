package com.example.taskmanager.dto;

import com.example.taskmanager.entity.InventoryUser;
import com.example.taskmanager.entity.UserRole;
import java.time.LocalDateTime;
import java.util.Set;
import com.example.taskmanager.entity.PermissionCode;

public record AccountResponse(Long id, String fullName, String username, UserRole role, boolean enabled, Set<PermissionCode> permissions, LocalDateTime createdAt) {
    public static AccountResponse from(InventoryUser user) {
        return new AccountResponse(user.getId(), user.getFullName(), user.getUsername(), user.getRole(), user.isEnabled(), Set.copyOf(user.getPermissions()), user.getCreatedAt());
    }
}
