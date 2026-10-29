package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(Long id, String title, String description, LocalDate dueDate, boolean completed, LocalDateTime createdAt) {
    public static TaskResponse from(Task t) { return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getDueDate(), t.isCompleted(), t.getCreatedAt()); }
}
