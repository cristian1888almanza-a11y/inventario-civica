package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Asset;
import com.example.taskmanager.entity.DepreciationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepreciationRecordRepository extends JpaRepository<DepreciationRecord, Long> {
    List<DepreciationRecord> findByAssetOrderByDepreciationDateDesc(Asset asset);

    List<DepreciationRecord> findByAssetAndPeriodYearAndPeriodMonth(Asset asset, Integer year, Integer month);

    @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.asset = :asset ORDER BY dr.depreciationDate DESC")
    List<DepreciationRecord> findByAssetOrderByDateDesc(@Param("asset") Asset asset);

    Optional<DepreciationRecord> findTopByAssetOrderByDepreciationDateDesc(Asset asset);

    List<DepreciationRecord> findByDepreciationDateBetween(LocalDate start, LocalDate end);

    List<DepreciationRecord> findByIsBatchProcessAndBatchReference(Boolean isBatchProcess, String batchReference);

    @Query("SELECT COUNT(dr) FROM DepreciationRecord dr WHERE dr.asset = :asset AND dr.periodYear = :year AND dr.periodMonth = :month")
    long countByAssetAndPeriod(@Param("asset") Asset asset, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.periodYear = :year AND dr.periodMonth = :month ORDER BY dr.depreciationDate DESC")
    List<DepreciationRecord> findByPeriodYearAndPeriodMonth(@Param("year") Integer year, @Param("month") Integer month);
}