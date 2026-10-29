package com.example.taskmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_created", columnList = "created_at"),
    @Index(name = "idx_activity_actor", columnList = "actor_username")
})
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String actorUsername;

    @Column(nullable = false, length = 120)
    private String actorName;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
