package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.entity.PermissionCode;
import java.util.Set;

public record AccountRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
    String fullName,
    @NotBlank(message = "El usuario es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "El usuario debe tener entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo")
    String username,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    String password,
    UserRole role,
    Set<PermissionCode> permissions
) {
    public AccountRequest(String fullName, String username, String password, UserRole role) {
        this(fullName, username, password, role, Set.of());
    }
}
