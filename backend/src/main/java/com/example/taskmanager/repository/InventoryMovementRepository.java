package com.example.taskmanager.repository;

import com.example.taskmanager.entity.InventoryMovement;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findAllByOrderByCreatedAtDesc(PageRequest pageRequest);
}
