package com.example.taskmanager.controller;

import com.example.taskmanager.dto.AssetCategoryRequest;
import com.example.taskmanager.dto.AssetCategoryResponse;
import com.example.taskmanager.service.AssetCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/asset-categories")
public class AssetCategoryController {
    private final AssetCategoryService service;

    public AssetCategoryController(AssetCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AssetCategoryResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AssetCategoryResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'CATEGORIAS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetCategoryResponse create(@Valid @RequestBody AssetCategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'CATEGORIAS_WRITE')")
    public AssetCategoryResponse update(@PathVariable Long id, @Valid @RequestBody AssetCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
