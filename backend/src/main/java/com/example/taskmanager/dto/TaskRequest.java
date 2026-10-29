package com.example.taskmanager.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede superar 100 caracteres")
    String title,
    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres") String description,
    @NotNull(message = "La fecha límite es obligatoria")
    @FutureOrPresent(message = "La fecha límite no puede estar en el pasado") LocalDate dueDate,
    boolean completed
) {}
