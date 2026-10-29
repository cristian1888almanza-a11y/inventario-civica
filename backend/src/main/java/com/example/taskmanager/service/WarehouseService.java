package com.example.taskmanager.service;

import com.example.taskmanager.dto.ProductStockResponse;
import com.example.taskmanager.dto.WarehouseResponse;
import com.example.taskmanager.entity.ProductStock;
import com.example.taskmanager.entity.Warehouse;
import com.example.taskmanager.repository.ProductRepository;
import com.example.taskmanager.repository.ProductStockRepository;
import com.example.taskmanager.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class WarehouseService {
    private final WarehouseRepository warehouses;
    private final ProductRepository products;
    private final ProductStockRepository stocks;

    public WarehouseService(WarehouseRepository warehouses, ProductRepository products, ProductStockRepository stocks) {
        this.warehouses = warehouses;
        this.products = products;
        this.stocks = stocks;
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> active() {
        return warehouses.findAllByActiveTrueOrderByNameAsc().stream().map(WarehouseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductStockResponse> stock(Long warehouseId) {
        requireWarehouse(warehouseId);
        return stocks.findAllByWarehouseIdOrderByProduct_Name(warehouseId).stream()
            .map(ProductStockResponse::from).toList();
    }

    @Transactional
    public WarehouseResponse create(String code, String name, String address) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (warehouses.existsByCodeIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Ya existe un almacén con el código " + normalized);
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(normalized);
        warehouse.setName(name.trim());
        warehouse.setAddress(address == null || address.isBlank() ? null : address.trim());
        return WarehouseResponse.from(warehouses.save(warehouse));
    }

    @Transactional
    public void ensureDefaultWarehouse() {
        Warehouse warehouse = warehouses.findByCodeIgnoreCase("MAIN").orElseGet(() -> {
            Warehouse created = new Warehouse();
            created.setCode("MAIN");
            created.setName("Almacén principal");
            return warehouses.save(created);
        });
        for (var product : products.findAll()) {
            if (stocks.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).isEmpty()) {
                ProductStock stock = new ProductStock();
                stock.setProduct(product);
                stock.setWarehouse(warehouse);
                stock.setQuantity(product.getStock());
                stocks.save(stock);
            }
        }
    }

    public Warehouse requireWarehouse(Long id) {
        return warehouses.findById(id).orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado: " + id));
    }

    @Transactional
    public void synchronizeDefaultStock(com.example.taskmanager.entity.Product product) {
        Warehouse warehouse = warehouses.findByCodeIgnoreCase("MAIN")
            .orElseThrow(() -> new IllegalStateException("No existe el almacén principal"));
        ProductStock stock = stocks.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
            .orElseGet(() -> {
                ProductStock created = new ProductStock();
                created.setProduct(product);
                created.setWarehouse(warehouse);
                return created;
            });
        stock.setQuantity(product.getStock());
        stocks.save(stock);
    }
}
