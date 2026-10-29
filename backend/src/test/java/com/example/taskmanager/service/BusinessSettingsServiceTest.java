package com.example.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmanager.dto.BusinessSettingsRequest;
import com.example.taskmanager.entity.BusinessSettingsRecord;
import com.example.taskmanager.repository.BusinessSettingsRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessSettingsServiceTest {
    @Mock
    private BusinessSettingsRepository repository;

    @Mock
    private ActivityLogService activityLog;

    @InjectMocks
    private BusinessSettingsService service;

    @Test
    void getReturnsPersistedSettings() {
        BusinessSettingsRecord settings = new BusinessSettingsRecord();
        settings.setBusinessName("Mi Tienda");
        settings.setCurrency("COP");
        when(repository.findById(1L)).thenReturn(Optional.of(settings));

        Map<String, String> result = service.get();

        assertEquals(Map.of("businessName", "Mi Tienda", "currency", "COP"), result);
    }

    @Test
    void getReturnsDefaultSettingsWhenNoRecordExists() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        Map<String, String> result = service.get();

        assertEquals(Map.of("businessName", "La Cívica", "currency", "USD"), result);
    }

    @Test
    void saveTrimsBusinessNameAndLogsTheUpdate() {
        BusinessSettingsRecord existing = new BusinessSettingsRecord();
        existing.setBusinessName("Viejo Nombre");
        existing.setCurrency("USD");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Map<String, String> result = service.save(new BusinessSettingsRequest("  Negocio Nuevo  ", "MXN"));

        assertEquals(Map.of("businessName", "Negocio Nuevo", "currency", "MXN"), result);
        assertEquals("Negocio Nuevo", existing.getBusinessName());
        assertEquals("MXN", existing.getCurrency());
        verify(activityLog).record(eq("SETTINGS_UPDATED"), eq("Se actualizaron los parámetros del negocio"));
    }
}
