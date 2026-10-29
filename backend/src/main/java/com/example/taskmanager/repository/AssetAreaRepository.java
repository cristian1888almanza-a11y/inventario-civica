package com.example.taskmanager.repository;

import com.example.taskmanager.entity.AssetArea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetAreaRepository extends JpaRepository<AssetArea, Long> {
    List<AssetArea> findAllBySite_IdOrderByNameAsc(Long siteId);
    Optional<AssetArea> findBySite_IdAndNameIgnoreCase(Long siteId, String name);
    Optional<AssetArea> findBySite_IdAndCode(Long siteId, String code);
}
