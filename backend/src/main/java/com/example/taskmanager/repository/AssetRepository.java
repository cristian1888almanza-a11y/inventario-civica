package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {}
