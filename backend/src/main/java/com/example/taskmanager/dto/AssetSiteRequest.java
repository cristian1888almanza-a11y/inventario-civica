package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetSiteRequest(
    @NotBlank @Size(max = 160) String name,
    @NotBlank @Size(max = 100) String department,
    @NotBlank @Size(max = 100) String city
) {}
