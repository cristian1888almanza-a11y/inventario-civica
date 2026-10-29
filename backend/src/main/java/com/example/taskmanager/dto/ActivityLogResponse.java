package com.example.taskmanager.dto;

import com.example.taskmanager.entity.ActivityLog;
import java.time.LocalDateTime;

public record ActivityLogResponse(Long id, String actorUsername, String actorName, String action,
                                  String detail, LocalDateTime createdAt) {
    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(log.getId(), log.getActorUsername(), log.getActorName(),
            log.getAction(), log.getDetail(), log.getCreatedAt());
    }
}
