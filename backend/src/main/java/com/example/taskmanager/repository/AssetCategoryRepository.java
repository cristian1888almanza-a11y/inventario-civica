package com.example.taskmanager.repository;

import com.example.taskmanager.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    Optional<AssetCategory> findByNameIgnoreCase(String name);
    List<AssetCategory> findAllByOrderByNameAsc();
}