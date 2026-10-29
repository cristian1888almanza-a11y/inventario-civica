package com.example.taskmanager.repository;

import com.example.taskmanager.entity.InventoryUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface InventoryUserRepository extends JpaRepository<InventoryUser, Long> {
    Optional<InventoryUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    List<InventoryUser> findAllByEnabledTrueOrderByFullNameAsc();
    List<InventoryUser> findAllByFullNameIgnoreCase(String fullName);
}
