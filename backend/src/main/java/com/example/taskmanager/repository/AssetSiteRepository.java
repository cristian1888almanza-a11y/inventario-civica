package com.example.taskmanager.repository;

import com.example.taskmanager.entity.AssetSite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetSiteRepository extends JpaRepository<AssetSite, Long> {
    List<AssetSite> findAllByCompany_IdOrderByNameAsc(Long companyId);
    Optional<AssetSite> findByCompany_IdAndNameIgnoreCase(Long companyId, String name);
    Optional<AssetSite> findByCompany_IdAndCode(Long companyId, String code);
}
