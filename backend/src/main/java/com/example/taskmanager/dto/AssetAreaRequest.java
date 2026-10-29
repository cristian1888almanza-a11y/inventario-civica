package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetAreaRequest(@NotBlank @Size(max = 100) String name) {}
