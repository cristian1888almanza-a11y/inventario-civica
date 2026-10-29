package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessSettingsRequest(@NotBlank @Size(max = 120) String businessName,
                                      @NotBlank @Pattern(regexp = "USD|MXN|COP|EUR", message = "Moneda no admitida") String currency) {
}
