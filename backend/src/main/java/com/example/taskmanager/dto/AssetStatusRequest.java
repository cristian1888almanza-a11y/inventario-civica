package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetStatusRequest(
    @NotBlank @Size(max = 40) String condition,
    @NotBlank @Size(max = 500) String reason
) {}