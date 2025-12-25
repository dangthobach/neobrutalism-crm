package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.dto.JobProgressInfo;
import com.neobrutalism.crm.application.migration.dto.ProgressInfo;
import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import com.neobrutalism.crm.application.migration.repository.MigrationJobRepository;
import com.neobrutalism.crm.application.migration.repository.MigrationSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for tracking migration progress in real-time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationProgressService {

    private final MigrationSheetRepository sheetRepository;
    private final MigrationJobRepository jobRepository;
    private final CacheManager cacheManager;

    // Counter to track batch updates - evict cache every 10 batches
    private final AtomicInteger batchUpdateCounter = new AtomicInteger(0);
    
    /**
     * Get real-time progress for a sheet
     */
    @Transactional(readOnly = true)
    public ProgressInfo getSheetProgress(UUID sheetId) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow(() -> new IllegalArgumentException("Sheet not found: " + sheetId));
        
        // Calculate progress percent
        double progressPercent = 0.0;
        if (sheet.getTotalRows() > 0) {
            progressPercent = (double) sheet.getProcessedRows() / sheet.getTotalRows() * 100.0;
        }
        
        // Calculate ETA
        Duration elapsed = Duration.ZERO;
        Duration estimatedRemaining = Duration.ZERO;
        
        if (sheet.getStartedAt() != null) {
            elapsed = Duration.between(sheet.getStartedAt(), Instant.now());
            
            if (sheet.getProcessedRows() > 0) {
                long remainingRows = sheet.getTotalRows() - sheet.getProcessedRows();
                long rowsPerSecond = sheet.getProcessedRows() / Math.max(1, elapsed.getSeconds());
                if (rowsPerSecond > 0) {
                    estimatedRemaining = Duration.ofSeconds(remainingRows / rowsPerSecond);
                }
            }
        }
        
        return ProgressInfo.builder()
            .sheetId(sheetId)
            .sheetName(sheet.getSheetName())
            .totalRows(sheet.getTotalRows())
            .processedRows(sheet.getProcessedRows())
            .validRows(sheet.getValidRows())
            .invalidRows(sheet.getInvalidRows())
            .skippedRows(sheet.getSkippedRows())
            .progressPercent(BigDecimal.valueOf(progressPercent)
                .setScale(2, RoundingMode.HALF_UP))
            .status(sheet.getStatus())
            .elapsedTime(elapsed)
            .estimatedRemaining(estimatedRemaining)
            .lastHeartbeat(sheet.getLastHeartbeat())
            .build();
    }
    
    /**
     * Get overall job progress with Redis caching
     * Cache key: "migration-progress::{jobId}"
     * TTL: 5 seconds (defined in cache config)
     */
    @Cacheable(value = "migration-progress", key = "#jobId", unless = "#result.status.terminal")
    @Transactional(readOnly = true)
    public JobProgressInfo getJobProgress(UUID jobId) {
        log.debug("Fetching job progress from database for job: {}", jobId);

        var job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<MigrationSheet> sheets = sheetRepository.findByJobId(jobId);
        
        long totalRows = sheets.stream()
            .mapToLong(MigrationSheet::getTotalRows)
            .sum();
        
        long processedRows = sheets.stream()
            .mapToLong(MigrationSheet::getProcessedRows)
            .sum();
        
        double overallProgress = totalRows > 0 
            ? (double) processedRows / totalRows * 100.0 
            : 0.0;
        
        List<ProgressInfo> sheetProgresses = sheets.stream()
            .map(sheet -> getSheetProgress(sheet.getId()))
            .collect(Collectors.toList());
        
        return JobProgressInfo.builder()
            .jobId(jobId)
            .fileName(job.getFileName())
            .totalSheets(sheets.size())
            .totalRows(totalRows)
            .processedRows(processedRows)
            .overallProgress(BigDecimal.valueOf(overallProgress)
                .setScale(2, RoundingMode.HALF_UP))
            .sheets(sheetProgresses)
            .status(job.getStatus())
            .build();
    }
    
    /**
     * Update sheet progress with smart cache eviction
     * Evicts cache every 10 batches to balance freshness and performance
     */
    @Transactional
    public void updateProgress(UUID sheetId, int batchNumber, int batchSize,
                              int validCount, int invalidCount) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow();

        sheet.setProcessedRows(sheet.getProcessedRows() + batchSize);
        sheet.setValidRows(sheet.getValidRows() + validCount);
        sheet.setInvalidRows(sheet.getInvalidRows() + invalidCount);
        sheet.setLastProcessedRow(sheet.getLastProcessedRow() + batchSize);
        sheet.setLastHeartbeat(Instant.now());

        // Calculate progress percent
        if (sheet.getTotalRows() > 0) {
            double progress = (double) sheet.getProcessedRows() / sheet.getTotalRows() * 100.0;
            sheet.setProgressPercent(BigDecimal.valueOf(progress)
                .setScale(2, RoundingMode.HALF_UP));
        }

        sheetRepository.save(sheet);

        // Evict cache every 10 batches to keep progress updates reasonably fresh
        // This reduces cache evictions from ~20 per sheet to ~2 per sheet
        if (batchUpdateCounter.incrementAndGet() % 10 == 0) {
            evictJobProgressCache(sheet.getJobId());
            log.debug("Evicted cache for job: {} after {} batches", sheet.getJobId(), batchUpdateCounter.get());
        }
    }

    /**
     * Manually evict cache for a specific job
     * Used when job status changes (completed, failed, cancelled)
     */
    @CacheEvict(value = "migration-progress", key = "#jobId")
    public void evictJobProgressCache(UUID jobId) {
        log.debug("Cache evicted for job: {}", jobId);
    }
}

