package com.example.taskmanager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.dto.AssetCategoryRequest;
import com.example.taskmanager.dto.AssetCategoryResponse;
import com.example.taskmanager.entity.AssetCategory;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.AssetCategoryRepository;

import jakarta.annotation.PostConstruct;

@Service
public class AssetCategoryService {
    private final AssetCategoryRepository repository;

    private static final String[] DEFAULT_CATEGORIES = {
        "Equipos de cómputo", "5", "0.20", "STRAIGHT_LINE", "Equipos de cómputo y periféricos",
        "Muebles y enseres", "10", "0.10", "STRAIGHT_LINE", "Muebles y enseres de oficina"
    };

    public AssetCategoryService(AssetCategoryRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void initializeDefaultCategories() {
        for (int i = 0; i < DEFAULT_CATEGORIES.length; i += 5) {
            String name = DEFAULT_CATEGORIES[i];
            if (repository.findByNameIgnoreCase(name).isEmpty()) {
                AssetCategory category = new AssetCategory();
                category.setName(name);
                category.setUsefulLifeYears(Integer.parseInt(DEFAULT_CATEGORIES[i + 1]));
                category.setAnnualDepreciationRate(new BigDecimal(DEFAULT_CATEGORIES[i + 2]));
                category.setDepreciationMethod(DEFAULT_CATEGORIES[i + 3]);
                category.setDescription(DEFAULT_CATEGORIES[i + 4]);
                repository.save(category);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> findAll() {
        return repository.findAllByOrderByNameAsc()
            .stream().map(AssetCategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssetCategoryResponse findById(Long id) {
        AssetCategory category = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
        return AssetCategoryResponse.from(category);
    }

    @Transactional
    public AssetCategoryResponse create(AssetCategoryRequest request) {
        if (repository.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + request.name());
        }
        AssetCategory category = new AssetCategory();
        apply(request, category);
        AssetCategory saved = repository.save(category);
        return AssetCategoryResponse.from(saved);
    }

    @Transactional
    public AssetCategoryResponse update(Long id, AssetCategoryRequest request) {
        AssetCategory category = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
        if (!category.getName().equalsIgnoreCase(request.name()) &&
            repository.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + request.name());
        }
        apply(request, category);
        AssetCategory updated = repository.save(category);
        return AssetCategoryResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        AssetCategory category = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
        repository.delete(category);
    }

    private void apply(AssetCategoryRequest request, AssetCategory category) {
        category.setName(request.name().trim());
        category.setUsefulLifeYears(request.usefulLifeYears());
        category.setAnnualDepreciationRate(BigDecimal.ONE.divide(
            BigDecimal.valueOf(request.usefulLifeYears()), 4, RoundingMode.HALF_UP));
        category.setDepreciationMethod(request.depreciationMethod() != null ? request.depreciationMethod() : "STRAIGHT_LINE");
        category.setDescription(request.description() != null ? request.description().trim() : null);
    }
}
