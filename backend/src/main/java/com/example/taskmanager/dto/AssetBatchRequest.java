package com.example.taskmanager.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AssetBatchRequest(@NotEmpty @Size(max = 500) List<@Valid AssetRequest> assets) {}
