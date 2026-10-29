package com.example.taskmanager.controller;

import com.example.taskmanager.dto.ProductStockResponse;
import com.example.taskmanager.dto.WarehouseResponse;
import com.example.taskmanager.service.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {
    private final WarehouseService service;

    public WarehouseController(WarehouseService service) { this.service = service; }

    @GetMapping
    public List<WarehouseResponse> active() { return service.active(); }

    @GetMapping("/{id}/stock")
    public List<ProductStockResponse> stock(@PathVariable Long id) { return service.stock(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@RequestBody CreateWarehouseRequest request) {
        return service.create(request.code(), request.name(), request.address());
    }

    public record CreateWarehouseRequest(String code, String name, String address) {}
}
