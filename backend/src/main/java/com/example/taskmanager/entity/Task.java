package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_completed_due_date", columnList = "completed, due_date")
})
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false)
    private boolean completed;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
