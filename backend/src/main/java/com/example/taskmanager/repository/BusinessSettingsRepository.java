package com.example.taskmanager.repository;

import com.example.taskmanager.entity.BusinessSettingsRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettingsRecord, Long> {
}
