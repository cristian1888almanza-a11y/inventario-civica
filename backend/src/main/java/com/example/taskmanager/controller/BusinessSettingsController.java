package com.example.taskmanager.controller;

import com.example.taskmanager.dto.BusinessSettingsRequest;
import com.example.taskmanager.service.BusinessSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/settings")
public class BusinessSettingsController {
    private final BusinessSettingsService service;

    public BusinessSettingsController(BusinessSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, String> get() {
        return service.get();
    }

    @PutMapping
    public Map<String, String> save(@Valid @RequestBody BusinessSettingsRequest request) {
        return service.save(request);
    }
}
