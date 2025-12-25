package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.model.MigrationJob;
import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import com.neobrutalism.crm.application.migration.model.MigrationStatus;
import com.neobrutalism.crm.application.migration.model.SheetStatus;
import com.neobrutalism.crm.application.migration.repository.MigrationJobRepository;
import com.neobrutalism.crm.application.migration.repository.MigrationSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for detecting and recovering stuck migration jobs/sheets
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StuckDetectionService {
    
    private final MigrationSheetRepository sheetRepository;
    private final MigrationJobRepository jobRepository;
    private final RecoveryService recoveryService;
    
    // Thresholds for stuck detection
    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(5); // No heartbeat for 5 minutes
    private static final Duration MAX_PROCESSING_TIME = Duration.ofHours(2); // Max 2 hours processing
    
    /**
     * Detect stuck sheets (no heartbeat for > threshold)
     * Runs every 30 seconds
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    @Transactional
    public void detectStuckSheets() {
        Instant threshold = Instant.now().minus(STUCK_THRESHOLD);
        
        List<MigrationSheet> stuckSheets = sheetRepository.findStuckSheets(threshold);
        
        if (stuckSheets.isEmpty()) {
            return;
        }
        
        log.warn("Detected {} stuck sheet(s)", stuckSheets.size());
        
        for (MigrationSheet sheet : stuckSheets) {
            log.warn("Stuck sheet detected: {} (job: {}, last heartbeat: {})", 
                     sheet.getId(), sheet.getJobId(), sheet.getLastHeartbeat());
            
            // Mark as stuck
            sheet.setStatus(SheetStatus.STUCK);
            sheet.setErrorMessage("Detected as stuck - no heartbeat for " + 
                                 Duration.between(sheet.getLastHeartbeat(), Instant.now()).toMinutes() + " minutes");
            sheetRepository.save(sheet);
            
            // Schedule recovery
            try {
                recoveryService.scheduleRecovery(sheet.getId());
            } catch (Exception e) {
                log.error("Failed to schedule recovery for stuck sheet: {}", sheet.getId(), e);
            }
        }
    }
    
    /**
     * Detect jobs that have been processing for too long
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void detectLongRunningJobs() {
        Instant maxStartTime = Instant.now().minus(MAX_PROCESSING_TIME);
        
        List<MigrationJob> longRunningJobs = jobRepository.findByStatus(MigrationStatus.PROCESSING)
            .stream()
            .filter(job -> job.getStartedAt() != null && job.getStartedAt().isBefore(maxStartTime))
            .toList();
        
        if (longRunningJobs.isEmpty()) {
            return;
        }
        
        log.warn("Detected {} long-running job(s)", longRunningJobs.size());
        
        for (MigrationJob job : longRunningJobs) {
            Duration processingTime = Duration.between(job.getStartedAt(), Instant.now());
            log.warn("Long-running job detected: {} (processing for {} minutes)", 
                     job.getId(), processingTime.toMinutes());
            
            // Check if all sheets are stuck
            List<MigrationSheet> sheets = sheetRepository.findByJobId(job.getId());
            boolean allSheetsStuck = sheets.stream()
                .allMatch(s -> s.getStatus() == SheetStatus.STUCK || s.getStatus() == SheetStatus.FAILED);
            
            if (allSheetsStuck && !sheets.isEmpty()) {
                log.warn("All sheets are stuck/failed for job: {}, marking job as STUCK", job.getId());
                job.setStatus(MigrationStatus.STUCK);
                job.setErrorMessage("All sheets are stuck or failed");
                jobRepository.save(job);
            }
        }
    }
    
    /**
     * Detect deadlocked or hung jobs
     * Checks for jobs with no activity for extended period
     * Runs every 10 minutes
     */
    @Scheduled(fixedDelay = 600000) // 10 minutes
    @Transactional
    public void detectDeadlockedJobs() {
        Instant deadlockThreshold = Instant.now().minus(Duration.ofMinutes(30));
        
        List<MigrationJob> deadlockedJobs = jobRepository.findByStatus(MigrationStatus.PROCESSING)
            .stream()
            .filter(job -> {
                if (job.getStartedAt() == null) {
                    return false;
                }
                
                // Check if any sheet has recent activity
                List<MigrationSheet> sheets = sheetRepository.findByJobId(job.getId());
                boolean hasRecentActivity = sheets.stream()
                    .anyMatch(s -> s.getLastHeartbeat() != null && 
                                 s.getLastHeartbeat().isAfter(deadlockThreshold));
                
                return !hasRecentActivity && sheets.stream()
                    .anyMatch(s -> s.getStatus() == SheetStatus.PROCESSING);
            })
            .toList();
        
        if (deadlockedJobs.isEmpty()) {
            return;
        }
        
        log.error("Detected {} potentially deadlocked job(s)", deadlockedJobs.size());
        
        for (MigrationJob job : deadlockedJobs) {
            log.error("Deadlocked job detected: {} (no activity for 30+ minutes)", job.getId());
            
            // Mark job and sheets as stuck
            job.setStatus(MigrationStatus.STUCK);
            job.setErrorMessage("Detected as deadlocked - no activity for 30+ minutes");
            jobRepository.save(job);
            
            List<MigrationSheet> sheets = sheetRepository.findByJobId(job.getId());
            for (MigrationSheet sheet : sheets) {
                if (sheet.getStatus() == SheetStatus.PROCESSING) {
                    sheet.setStatus(SheetStatus.STUCK);
                    sheet.setErrorMessage("Detected as deadlocked");
                    sheetRepository.save(sheet);
                    
                    // Schedule recovery
                    try {
                        recoveryService.scheduleRecovery(sheet.getId());
                    } catch (Exception e) {
                        log.error("Failed to schedule recovery for deadlocked sheet: {}", sheet.getId(), e);
                    }
                }
            }
        }
    }
}

