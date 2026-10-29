package com.example.taskmanager.service;

import com.example.taskmanager.dto.BusinessSettingsRequest;
import com.example.taskmanager.entity.BusinessSettingsRecord;
import com.example.taskmanager.repository.BusinessSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class BusinessSettingsService {
    private final BusinessSettingsRepository repository;
    private final ActivityLogService activityLog;

    public BusinessSettingsService(BusinessSettingsRepository repository, ActivityLogService activityLog) {
        this.repository = repository;
        this.activityLog = activityLog;
    }

    @Transactional(readOnly = true)
    public Map<String, String> get() {
        BusinessSettingsRecord settings = repository.findById(1L).orElseGet(BusinessSettingsRecord::new);
        return Map.of("businessName", settings.getBusinessName(), "currency", settings.getCurrency());
    }

    @Transactional
    public Map<String, String> save(BusinessSettingsRequest request) {
        BusinessSettingsRecord settings = repository.findById(1L).orElseGet(BusinessSettingsRecord::new);
        settings.setBusinessName(request.businessName().trim());
        settings.setCurrency(request.currency());
        BusinessSettingsRecord saved = repository.save(settings);
        activityLog.record("SETTINGS_UPDATED", "Se actualizaron los parámetros del negocio");
        return Map.of("businessName", saved.getBusinessName(), "currency", saved.getCurrency());
    }
}
