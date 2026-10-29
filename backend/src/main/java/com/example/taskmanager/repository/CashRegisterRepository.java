package com.example.taskmanager.repository;

import com.example.taskmanager.entity.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CashRegister> findFirstByStatusOrderByOpenedAtDesc(String status);

    @Query("select r from CashRegister r where r.status = 'OPEN' order by r.openedAt desc")
    List<CashRegister> findOpenForSale(Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("update CashRegister r set r.balance = r.balance + :amount where r.id = :id and r.status = 'OPEN'")
    int addBalanceIfOpen(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
