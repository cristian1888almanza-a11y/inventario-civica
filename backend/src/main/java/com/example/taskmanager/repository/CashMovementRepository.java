package com.example.taskmanager.repository;

import com.example.taskmanager.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findAllByRegisterIdOrderByCreatedAtDesc(Long registerId);
}
