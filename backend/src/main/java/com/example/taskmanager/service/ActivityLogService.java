package com.example.taskmanager.service;

import com.example.taskmanager.dto.ActivityLogResponse;
import com.example.taskmanager.entity.ActivityLog;
import com.example.taskmanager.repository.ActivityLogRepository;
import com.example.taskmanager.repository.InventoryUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ActivityLogService {
    private static final int MAX_ENTRIES = 300;
    private final ActivityLogRepository repository;
    private final InventoryUserRepository users;

    public ActivityLogService(ActivityLogRepository repository, InventoryUserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional
    public void record(String actorUsername, String action, String detail) {
        String normalizedUsername = actorUsername.trim();
        String actorName = users.findByUsernameIgnoreCase(normalizedUsername)
            .map(user -> user.getFullName())
            .orElse(normalizedUsername);
        ActivityLog log = new ActivityLog();
        log.setActorUsername(normalizedUsername);
        log.setActorName(actorName);
        log.setAction(action);
        log.setDetail(detail);
        repository.save(log);
    }

    @Transactional
    public void record(String action, String detail) {
        record(currentUsername(), action, detail);
    }

    public String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("No se pudo identificar al usuario para registrar la actividad");
        }
        return authentication.getName();
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> recent() {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_ENTRIES))
            .stream().map(ActivityLogResponse::from).toList();
    }
}
