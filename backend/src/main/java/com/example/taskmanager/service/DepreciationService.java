package com.example.taskmanager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.taskmanager.dto.AssetDepreciationValuationResponse;
import com.example.taskmanager.dto.DepreciationBatchRequest;
import com.example.taskmanager.dto.DepreciationCalculateRequest;
import com.example.taskmanager.dto.DepreciationRecordResponse;
import com.example.taskmanager.dto.DepreciationReportResponse;
import com.example.taskmanager.entity.Asset;
import com.example.taskmanager.entity.AssetCategory;
import com.example.taskmanager.entity.AssetStatus;
import com.example.taskmanager.entity.DepreciationRecord;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.AssetRepository;
import com.example.taskmanager.repository.DepreciationRecordRepository;

@Service
public class DepreciationService {
    private static final Logger log = LoggerFactory.getLogger(DepreciationService.class);
    private final AssetRepository assetRepository;
    private final DepreciationRecordRepository depreciationRepository;

    public DepreciationService(AssetRepository assetRepository, DepreciationRecordRepository depreciationRepository) {
        this.assetRepository = assetRepository;
        this.depreciationRepository = depreciationRepository;
    }

    /**
     * Ejecuta el cierre mensual sin intervención del usuario. El proceso es
     * idempotente: si el período ya fue procesado, no crea registros duplicados.
     * La expresión se puede cambiar con app.depreciation.cron.
     */
    @Scheduled(cron = "${app.depreciation.cron:0 15 2 1 * *}", zone = "${app.depreciation.zone:America/Bogota}")
    @Transactional
    public void runAutomaticDepreciation() {
        LocalDate today = LocalDate.now();
        try {
            depreciateAll(new DepreciationBatchRequest(today, "SYSTEM"));
            log.info("Depreciación automática ejecutada para {}", today);
        } catch (RuntimeException exception) {
            log.error("No se pudo ejecutar la depreciación automática para {}", today, exception);
        }
    }

    @Transactional
    public DepreciationRecordResponse calculateDepreciation(DepreciationCalculateRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado: " + request.assetId()));

        validateAssetForDepreciation(asset, request.depreciationDate());

        List<DepreciationRecord> periodRecords = depreciationRepository.findByAssetAndPeriodYearAndPeriodMonth(
            asset, request.depreciationDate().getYear(), request.depreciationDate().getMonthValue());
        if (!periodRecords.isEmpty()) {
            return DepreciationRecordResponse.from(periodRecords.stream()
                .max(java.util.Comparator.comparing(DepreciationRecord::getDepreciationDate))
                .orElseThrow());
        }

        DepreciationRecord record = computeDepreciation(asset, request.depreciationDate(), request.processedBy(), false, null);
        DepreciationRecord saved = depreciationRepository.save(record);
        updateAssetDepreciation(asset, record);
        return DepreciationRecordResponse.from(saved);
    }

    @Transactional
    public DepreciationReportResponse depreciateAll(DepreciationBatchRequest request) {
        if (request.depreciationDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de corte no puede ser posterior a hoy.");
        }
        List<Asset> activeAssets = assetRepository.findAll().stream()
            .filter(a -> a.getAssetStatus() == AssetStatus.ACTIVE && a.getCategory() != null)
            .filter(a -> a.getStartUseDate() != null && !a.getStartUseDate().isAfter(request.depreciationDate()))
            .filter(a -> a.getPurchaseValue() != null && a.getPurchaseValue().compareTo(BigDecimal.ZERO) > 0)
            .filter(a -> a.getUsefulLifeYears() != null ? a.getUsefulLifeYears() > 0 : a.getCategory().getUsefulLifeYears() > 0)
            .filter(a -> a.getResidualValue() == null || (a.getResidualValue().signum() >= 0
                && a.getResidualValue().compareTo(a.getPurchaseValue()) <= 0))
            .filter(a -> depreciationRepository.findByAssetAndPeriodYearAndPeriodMonth(
                a, request.depreciationDate().getYear(), request.depreciationDate().getMonthValue()).isEmpty())
            .toList();

        if (activeAssets.isEmpty()) {
            return new DepreciationReportResponse(
                request.depreciationDate().getYear(),
                request.depreciationDate().getMonthValue(),
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()
            );
        }

        String batchRef = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<DepreciationRecord> records = activeAssets.stream()
            .map(asset -> computeDepreciation(asset, request.depreciationDate(), request.processedBy(), true, batchRef))
            .toList();

        List<DepreciationRecord> savedRecords = depreciationRepository.saveAll(records);
        activeAssets.forEach(this::recalculateAssetBookValue);

        BigDecimal totalMonthly = savedRecords.stream()
            .map(DepreciationRecord::getMonthlyDepreciation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccumulated = savedRecords.stream()
            .map(DepreciationRecord::getAccumulatedDepreciation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBookValue = activeAssets.stream()
            .map(Asset::getBookValue)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DepreciationReportResponse(
            request.depreciationDate().getYear(),
            request.depreciationDate().getMonthValue(),
            activeAssets.size(),
            totalMonthly,
            totalAccumulated,
            totalBookValue,
            savedRecords.stream().map(DepreciationRecordResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<DepreciationRecordResponse> getAssetHistory(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado: " + assetId));
        return depreciationRepository.findByAssetOrderByDateDesc(asset)
            .stream().map(DepreciationRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AssetDepreciationValuationResponse> getValuation(LocalDate valuationDate) {
        LocalDate asOf = valuationDate == null ? LocalDate.now() : valuationDate;
        if (asOf.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de corte no puede ser posterior a hoy.");
        }
        return assetRepository.findAll().stream().map(asset -> {
            AssetCategory category = asset.getCategory();
            Integer usefulLifeYears = asset.getUsefulLifeYears() != null
                ? asset.getUsefulLifeYears()
                : category == null ? null : category.getUsefulLifeYears();
            BigDecimal annualRate = category == null ? null : category.getAnnualDepreciationRate();
            BigDecimal purchaseValue = asset.getPurchaseValue();
            BigDecimal residualValue = asset.getResidualValue() == null ? BigDecimal.ZERO : asset.getResidualValue();
            BigDecimal monthly = BigDecimal.ZERO;
            BigDecimal accumulated = BigDecimal.ZERO;
            BigDecimal bookValue = purchaseValue;
            AssetStatus status = asset.getAssetStatus();

            if (purchaseValue != null && usefulLifeYears != null && usefulLifeYears > 0) {
                int totalMonths = usefulLifeYears * 12;
                BigDecimal depreciableBase = purchaseValue.subtract(residualValue).max(BigDecimal.ZERO);
                monthly = depreciableBase.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
                if (asset.getStartUseDate() != null && !asset.getStartUseDate().isAfter(asOf)) {
                    int elapsedMonthsTotal = monthsBetween(asset.getStartUseDate(), asOf);
                    int elapsedMonths = Math.min(elapsedMonthsTotal, totalMonths);
                    accumulated = depreciableBase.multiply(BigDecimal.valueOf(elapsedMonths))
                        .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP)
                        .min(depreciableBase);
                    bookValue = purchaseValue.subtract(accumulated).max(residualValue);
                    if (accumulated.compareTo(depreciableBase) >= 0) {
                        status = AssetStatus.FULLY_DEPRECIATED;
                    }
                }
            }

            return AssetDepreciationValuationResponse.from(
                asset, asOf, annualRate, monthly, accumulated, bookValue, status);
        }).toList();
    }

    @Transactional(readOnly = true)
    public DepreciationReportResponse getPeriodReport(Integer year, Integer month) {
        List<DepreciationRecord> records = depreciationRepository.findByPeriodYearAndPeriodMonth(year, month);
        
        BigDecimal totalMonthly = records.stream()
            .map(DepreciationRecord::getMonthlyDepreciation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccumulated = records.stream()
            .map(DepreciationRecord::getAccumulatedDepreciation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBookValue = records.stream()
            .map(DepreciationRecord::getBookValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DepreciationReportResponse(
            year, month, records.size(),
            totalMonthly, totalAccumulated, totalBookValue,
            records.stream().map(DepreciationRecordResponse::from).toList()
        );
    }

    private void validateAssetForDepreciation(Asset asset, LocalDate depreciationDate) {
        if (asset.getAssetStatus() != AssetStatus.ACTIVE) {
            throw new IllegalStateException("El bien no está activo (estado: " + asset.getAssetStatus() + ")");
        }
        if (asset.getCategory() == null) {
            throw new IllegalStateException("El bien no tiene categoría asignada");
        }
        if (asset.getStartUseDate() == null) {
            throw new IllegalStateException("El bien no tiene fecha de inicio de uso");
        }
        if (asset.getStartUseDate().isAfter(depreciationDate)) {
            throw new IllegalStateException("La fecha de depreciación es anterior a la fecha de inicio de uso");
        }
        if (depreciationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de corte no puede ser posterior a hoy.");
        }
        if (asset.getPurchaseValue() == null || asset.getPurchaseValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El bien no tiene valor de compra válido");
        }
        BigDecimal residualValue = asset.getResidualValue() == null ? BigDecimal.ZERO : asset.getResidualValue();
        if (residualValue.signum() < 0 || residualValue.compareTo(asset.getPurchaseValue()) > 0) {
            throw new IllegalStateException("El valor residual debe estar entre cero y el valor de adquisición");
        }
        Integer usefulLifeYears = asset.getUsefulLifeYears() != null
            ? asset.getUsefulLifeYears()
            : asset.getCategory().getUsefulLifeYears();
        if (usefulLifeYears == null || usefulLifeYears <= 0) {
            throw new IllegalStateException("La categoría no tiene una vida útil válida");
        }
    }

    private DepreciationRecord computeDepreciation(Asset asset, LocalDate depreciationDate, String processedBy, boolean isBatch, String batchRef) {
        AssetCategory category = asset.getCategory();
        BigDecimal purchaseValue = asset.getPurchaseValue();
        BigDecimal residualValue = asset.getResidualValue() != null ? asset.getResidualValue() : BigDecimal.ZERO;
        Integer usefulLifeYears = asset.getUsefulLifeYears() != null ? asset.getUsefulLifeYears() : category.getUsefulLifeYears();

        int totalMonths = usefulLifeYears * 12;
        BigDecimal depreciableBase = purchaseValue.subtract(residualValue);
        int monthsSinceStart = Math.min(monthsBetween(asset.getStartUseDate(), depreciationDate), totalMonths);
        BigDecimal newAccumulated = depreciableBase.multiply(BigDecimal.valueOf(monthsSinceStart))
            .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP)
            .min(depreciableBase);
        BigDecimal previousAccumulated = depreciableBase.multiply(BigDecimal.valueOf(Math.max(0, monthsSinceStart - 1)))
            .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP)
            .min(depreciableBase);
        BigDecimal monthlyDepreciation = newAccumulated.subtract(previousAccumulated);

        BigDecimal bookValue = purchaseValue.subtract(newAccumulated);
        if (bookValue.compareTo(residualValue) < 0) {
            bookValue = residualValue;
        }

        if (newAccumulated.compareTo(depreciableBase) >= 0) {
            asset.setAssetStatus(AssetStatus.FULLY_DEPRECIATED);
        }

        DepreciationRecord record = new DepreciationRecord();
        record.setAsset(asset);
        record.setDepreciationDate(depreciationDate);
        record.setPeriodYear(depreciationDate.getYear());
        record.setPeriodMonth(depreciationDate.getMonthValue());
        record.setMonthlyDepreciation(monthlyDepreciation);
        record.setAccumulatedDepreciation(newAccumulated);
        record.setBookValue(bookValue);
        record.setProcessedBy(processedBy);
        record.setIsBatchProcess(isBatch);
        record.setBatchReference(batchRef);

        return record;
    }

    private void updateAssetDepreciation(Asset asset, DepreciationRecord record) {
        asset.setAccumulatedDepreciation(record.getAccumulatedDepreciation());
        asset.setBookValue(record.getBookValue());
        if (record.getAccumulatedDepreciation().compareTo(
            asset.getPurchaseValue().subtract(asset.getResidualValue() != null ? asset.getResidualValue() : BigDecimal.ZERO)) >= 0) {
            asset.setAssetStatus(AssetStatus.FULLY_DEPRECIATED);
        }
        assetRepository.save(asset);
    }

    private void recalculateAssetBookValue(Asset asset) {
        DepreciationRecord lastRecord = depreciationRepository.findTopByAssetOrderByDepreciationDateDesc(asset).orElse(null);
        if (lastRecord != null) {
            asset.setAccumulatedDepreciation(lastRecord.getAccumulatedDepreciation());
            asset.setBookValue(lastRecord.getBookValue());
            if (asset.getPurchaseValue() != null && asset.getResidualValue() != null) {
                BigDecimal depreciableBase = asset.getPurchaseValue().subtract(asset.getResidualValue());
                if (lastRecord.getAccumulatedDepreciation().compareTo(depreciableBase) >= 0) {
                    asset.setAssetStatus(AssetStatus.FULLY_DEPRECIATED);
                }
            }
            assetRepository.save(asset);
        }
    }

    private int monthsBetween(LocalDate start, LocalDate end) {
        return Math.max(0, (int) ChronoUnit.MONTHS.between(start, end));
    }
}
