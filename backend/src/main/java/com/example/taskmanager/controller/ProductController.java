package com.example.taskmanager.controller;

import com.example.taskmanager.dto.InventorySummary;
import com.example.taskmanager.dto.ProductRequest;
import com.example.taskmanager.dto.ProductResponse;
import com.example.taskmanager.report.ExcelReportWriter;
import com.example.taskmanager.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/summary")
    public InventorySummary getSummary() {
        return service.summary();
    }

    @GetMapping(value = "/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReport() {
        byte[] report = service.exportReport();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-inventario.pdf").build().toString())
            .body(report);
    }

    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel() {
        List<ProductResponse> products = service.findAll();
        byte[] report = ExcelReportWriter.write(
            List.of("SKU", "Producto", "Categoría", "Precio", "Existencias", "Stock mínimo", "Proveedor"),
            products.stream()
                .map(product -> Arrays.<Object>asList(
                    product.sku(), product.name(), product.category(), product.price(),
                    product.stock(), product.minStock(), product.supplier()))
                .toList()
        );
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-inventario.xlsx").build().toString())
            .body(report);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
