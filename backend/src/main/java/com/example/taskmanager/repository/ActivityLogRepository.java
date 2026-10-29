package com.example.taskmanager.repository;

import com.example.taskmanager.entity.ActivityLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findAllByOrderByCreatedAtDesc(PageRequest pageRequest);
}
