package com.example.taskmanager.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WarehouseDataInitializer implements ApplicationRunner {
    private final WarehouseService service;

    public WarehouseDataInitializer(WarehouseService service) { this.service = service; }

    @Override
    public void run(ApplicationArguments args) {
        service.ensureDefaultWarehouse();
    }
}
