package com.example.taskmanager.repository;

import com.example.taskmanager.entity.OperationRecord;
import com.example.taskmanager.entity.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<OperationRecord, Long> {
    List<OperationRecord> findAllByTypeOrderByCreatedAtDesc(OperationType type);
    Optional<OperationRecord> findByReceiptNumber(String receiptNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperationRecord o where o.id = :id")
    Optional<OperationRecord> findByIdForUpdate(@Param("id") Long id);
}
