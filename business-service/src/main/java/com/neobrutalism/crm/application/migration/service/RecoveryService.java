package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import com.neobrutalism.crm.application.migration.model.SheetStatus;
import com.neobrutalism.crm.application.migration.repository.MigrationSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for recovering stuck migration sheets
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryService {
    
    private final MigrationSheetRepository sheetRepository;
    private final ExcelMigrationService migrationService;
    
    /**
     * Schedule recovery for a stuck sheet
     */
    @Async("excelMigrationExecutor")
    public CompletableFuture<Void> scheduleRecovery(UUID sheetId) {
        log.info("Scheduling recovery for stuck sheet: {}", sheetId);
        
        try {
            recoverSheet(sheetId);
        } catch (Exception e) {
            log.error("Recovery failed for sheet: {}", sheetId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Recover a stuck sheet by resuming from last processed row
     */
    @Transactional
    public void recoverSheet(UUID sheetId) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow(() -> new IllegalArgumentException("Sheet not found: " + sheetId));
        
        if (sheet.getStatus() != SheetStatus.STUCK) {
            log.warn("Sheet {} is not in STUCK status, skipping recovery", sheetId);
            return;
        }
        
        log.info("Recovering stuck sheet: {} (last processed row: {})", 
                 sheetId, sheet.getLastProcessedRow());
        
        // Check if sheet is actually stuck or just slow
        if (sheet.getLastHeartbeat() != null) {
            long minutesSinceHeartbeat = java.time.Duration.between(
                sheet.getLastHeartbeat(), 
                Instant.now()
            ).toMinutes();
            
            if (minutesSinceHeartbeat < 10) {
                log.info("Sheet {} heartbeat is recent ({} minutes ago), may not be stuck", 
                         sheetId, minutesSinceHeartbeat);
                // Don't recover if heartbeat is recent
                return;
            }
        }
        
        // Reset sheet status and resume processing
        sheet.setStatus(SheetStatus.PROCESSING);
        sheet.setLastHeartbeat(Instant.now());
        sheet.setErrorMessage(null);
        sheetRepository.save(sheet);
        
        log.info("Resuming processing for sheet: {} from row {}",
                 sheetId, sheet.getLastProcessedRow());

        // Resume processing by calling processSheet again
        // It will check lastProcessedRow and continue from there
        try {
            migrationService.processSheet(sheetId);
            log.info("Successfully resumed processing for sheet: {}", sheetId);
        } catch (Exception e) {
            log.error("Failed to resume processing for sheet: {}", sheetId, e);
            sheet.setStatus(SheetStatus.FAILED);
            sheet.setErrorMessage("Recovery failed: " + e.getMessage());
            sheetRepository.save(sheet);
        }
    }
    
    /**
     * Manual recovery trigger (for admin use)
     */
    @Transactional
    public void manualRecoverSheet(UUID sheetId) {
        log.info("Manual recovery triggered for sheet: {}", sheetId);
        recoverSheet(sheetId);
    }
    
    /**
     * Recover all stuck sheets for a job
     */
    @Transactional
    public void recoverJob(UUID jobId) {
        log.info("Recovering all stuck sheets for job: {}", jobId);
        
        List<MigrationSheet> stuckSheets = sheetRepository.findByJobId(jobId)
            .stream()
            .filter(s -> s.getStatus() == SheetStatus.STUCK)
            .toList();
        
        log.info("Found {} stuck sheet(s) to recover for job: {}", stuckSheets.size(), jobId);
        
        for (MigrationSheet sheet : stuckSheets) {
            try {
                recoverSheet(sheet.getId());
            } catch (Exception e) {
                log.error("Failed to recover sheet: {} for job: {}", sheet.getId(), jobId, e);
            }
        }
    }
}

