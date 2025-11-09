package com.neobrutalism.crm.application.migration.controller;

import com.neobrutalism.crm.application.migration.dto.JobProgressInfo;
import com.neobrutalism.crm.application.migration.dto.MigrationErrorResponse;
import com.neobrutalism.crm.application.migration.model.MigrationJob;
import com.neobrutalism.crm.application.migration.service.ExcelMigrationService;
import com.neobrutalism.crm.application.migration.service.MigrationErrorService;
import com.neobrutalism.crm.application.migration.service.MigrationProgressService;
import com.neobrutalism.crm.application.migration.service.RecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Excel migration
 */
@Slf4j
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final ExcelMigrationService migrationService;
    private final MigrationProgressService progressService;
    private final MigrationErrorService errorService;
    private final RecoveryService recoveryService;

    @Qualifier("fileUploadExecutor")
    private final ThreadPoolTaskExecutor fileUploadExecutor;

    /**
     * Upload Excel file and start migration (Async)
     * Uses DeferredResult for async processing to prevent request timeout on large files
     * Returns 202 Accepted immediately and processes file in background
     */
    @PostMapping("/upload")
    public DeferredResult<ResponseEntity<MigrationJob>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Received file upload request: {} ({} bytes)",
                 file.getOriginalFilename(), file.getSize());

        // Create DeferredResult with 5 minute timeout
        DeferredResult<ResponseEntity<MigrationJob>> deferredResult =
            new DeferredResult<>(300000L); // 5 minutes

        // Set timeout handler
        deferredResult.onTimeout(() -> {
            log.error("Upload request timed out for file: {}", file.getOriginalFilename());
            deferredResult.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(null)
            );
        });

        // Process upload asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                // Create migration job
                log.info("Creating migration job for file: {}", file.getOriginalFilename());
                MigrationJob job = migrationService.createMigrationJob(file);

                // Start migration asynchronously
                log.info("Starting migration for job: {}", job.getId());
                migrationService.startMigration(job.getId());

                return ResponseEntity.accepted().body(job);

            } catch (Exception e) {
                log.error("Error processing file upload: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Failed to process file upload", e);
            }
        }, fileUploadExecutor)
        .whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Upload processing failed", throwable);
                deferredResult.setErrorResult(
                    ResponseEntity.internalServerError().build()
                );
            } else {
                deferredResult.setResult(result);
            }
        });

        return deferredResult;
    }
    
    /**
     * Get migration job by ID
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<MigrationJob> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(migrationService.getJob(jobId));
    }
    
    /**
     * Get progress for a migration job (one-time)
     */
    @GetMapping("/jobs/{jobId}/progress")
    public ResponseEntity<JobProgressInfo> getProgress(@PathVariable UUID jobId) {
        JobProgressInfo progress = progressService.getJobProgress(jobId);
        return ResponseEntity.ok(progress);
    }
    
    /**
     * Stream progress updates via Server-Sent Events (SSE)
     * @deprecated Use WebSocket instead for better performance
     * WebSocket endpoint: /ws/migration
     * Subscribe to: /topic/migration/{jobId}
     */
    @Deprecated
    @GetMapping(value = "/jobs/{jobId}/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<JobProgressInfo>> streamProgress(@PathVariable UUID jobId) {
        log.warn("SSE endpoint called - consider migrating to WebSocket for better performance");
        return Flux.interval(Duration.ofSeconds(1))
            .map(seq -> {
                try {
                    JobProgressInfo progress = progressService.getJobProgress(jobId);
                    return ServerSentEvent.<JobProgressInfo>builder()
                        .id(String.valueOf(seq))
                        .event("progress")
                        .data(progress)
                        .build();
                } catch (Exception e) {
                    log.error("Error getting progress for job: {}", jobId, e);
                    return ServerSentEvent.<JobProgressInfo>builder()
                        .id(String.valueOf(seq))
                        .event("error")
                        .data(null)
                        .build();
                }
            })
            .takeWhile(event -> {
                if (event.data() == null) {
                    return false;
                }
                return !event.data().getStatus().isTerminal();
            })
            .onErrorResume(e -> {
                log.error("Error in progress stream", e);
                return Flux.empty();
            });
    }
    
    /**
     * Cancel a migration job
     */
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelMigration(@PathVariable UUID jobId) {
        migrationService.cancelMigration(jobId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get validation errors for a job
     */
    @GetMapping("/jobs/{jobId}/errors")
    public ResponseEntity<List<MigrationErrorResponse>> getJobErrors(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        List<MigrationErrorResponse> errors = errorService.getJobErrors(jobId, page, size);
        return ResponseEntity.ok(errors);
    }
    
    /**
     * Get validation errors for a specific sheet
     */
    @GetMapping("/sheets/{sheetId}/errors")
    public ResponseEntity<MigrationErrorResponse> getSheetErrors(
            @PathVariable UUID sheetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        MigrationErrorResponse errors = errorService.getSheetErrors(sheetId, page, size);
        return ResponseEntity.ok(errors);
    }
    
    /**
     * Manually trigger recovery for a stuck sheet
     */
    @PostMapping("/sheets/{sheetId}/recover")
    public ResponseEntity<Void> recoverSheet(@PathVariable UUID sheetId) {
        recoveryService.manualRecoverSheet(sheetId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Manually trigger recovery for all stuck sheets in a job
     */
    @PostMapping("/jobs/{jobId}/recover")
    public ResponseEntity<Void> recoverJob(@PathVariable UUID jobId) {
        recoveryService.recoverJob(jobId);
        return ResponseEntity.ok().build();
    }
}

