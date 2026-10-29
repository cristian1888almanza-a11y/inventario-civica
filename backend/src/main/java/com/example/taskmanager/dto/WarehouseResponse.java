package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Warehouse;

public record WarehouseResponse(Long id, String code, String name, String address, boolean active) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(),
            warehouse.getAddress(), warehouse.isActive());
    }
}
