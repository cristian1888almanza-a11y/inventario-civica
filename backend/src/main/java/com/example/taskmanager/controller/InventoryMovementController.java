package com.example.taskmanager.controller;

import com.example.taskmanager.dto.InventoryMovementRequest;
import com.example.taskmanager.dto.InventoryMovementResponse;
import com.example.taskmanager.service.InventoryMovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/inventory/movements")
public class InventoryMovementController {
    private final InventoryMovementService service;

    public InventoryMovementController(InventoryMovementService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryMovementResponse> recent() {
        return service.recent();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse adjust(@Valid @RequestBody InventoryMovementRequest request) {
        return service.adjust(request);
    }
}
