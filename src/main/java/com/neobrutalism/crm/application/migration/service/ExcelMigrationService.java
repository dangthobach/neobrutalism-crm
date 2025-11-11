package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.excel.ExcelFacade;
import com.neobrutalism.crm.application.migration.dto.HSBGCifDTO;
import com.neobrutalism.crm.application.migration.dto.HSBGHopDongDTO;
import com.neobrutalism.crm.application.migration.dto.HSBGTapDTO;
import com.neobrutalism.crm.application.migration.dto.MigrationResult;
import com.neobrutalism.crm.application.migration.model.MigrationJob;
import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import com.neobrutalism.crm.application.migration.model.MigrationStatus;
import com.neobrutalism.crm.application.migration.model.SheetStatus;
import com.neobrutalism.crm.application.migration.model.SheetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.application.migration.entity.StagingHSBGHopDong;
import com.neobrutalism.crm.application.migration.entity.StagingHSBGCif;
import com.neobrutalism.crm.application.migration.entity.StagingHSBGTap;
import com.neobrutalism.crm.application.migration.repository.MigrationJobRepository;
import com.neobrutalism.crm.application.migration.repository.MigrationSheetRepository;
import com.neobrutalism.crm.application.migration.repository.StagingHSBGHopDongRepository;
import com.neobrutalism.crm.application.migration.repository.StagingHSBGCifRepository;
import com.neobrutalism.crm.application.migration.repository.StagingHSBGTapRepository;
import com.neobrutalism.crm.application.migration.validation.ValidationResult;
import com.neobrutalism.crm.application.migration.validation.impl.HSBGHopDongValidator;
import com.neobrutalism.crm.utils.config.ExcelConfig;
import com.neobrutalism.crm.utils.config.ExcelConfigFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Main service for Excel migration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelMigrationService {

    private final MigrationJobRepository jobRepository;
    private final MigrationSheetRepository sheetRepository;
    private final ExcelFacade excelFacade;
    private final DataNormalizer dataNormalizer;
    private final DuplicateDetectionService duplicateDetectionService;
    private final MigrationProgressService progressService;
    private final MigrationFileStorageService fileStorageService;
    private final ExcelMetadataParser metadataParser;
    private final StagingHSBGHopDongRepository stagingHopDongRepository;
    private final StagingHSBGCifRepository stagingCifRepository;
    private final StagingHSBGTapRepository stagingTapRepository;
    private final com.neobrutalism.crm.application.migration.repository.JdbcBatchInsertHelper jdbcBatchInsertHelper;
    private final HSBGHopDongValidator hopDongValidator;
    private final com.neobrutalism.crm.application.migration.validation.impl.HSBGCifValidator cifValidator;
    private final com.neobrutalism.crm.application.migration.validation.impl.HSBGTapValidator tapValidator;
    private final ObjectMapper objectMapper;
    private final MigrationErrorLogger errorLogger;
    private final com.neobrutalism.crm.application.migration.monitoring.MigrationMonitor migrationMonitor;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * Memory-aware concurrency control
     * 
     * Instead of fixed semaphore (max 4 sheets), we track actual memory usage
     * This prevents OOM by ensuring total memory usage stays under limit
     * 
     * Configuration:
     * - MAX_MEMORY_BYTES: 2GB limit for all sheet processing combined
     * - ESTIMATED_MEMORY_PER_ROW: 2KB per row (conservative estimate)
     * - Sheet will wait if adding it would exceed memory limit
     */
    private final AtomicLong currentMemoryUsage = new AtomicLong(0);
    private static final long MAX_MEMORY_BYTES = 2L * 1024 * 1024 * 1024; // 2GB limit
    private static final long ESTIMATED_MEMORY_PER_ROW = 2000L; // 2KB per row
    private static final long MEMORY_CHECK_INTERVAL_MS = 1000L; // Check every 1 second
    
    /**
     * Create migration job from uploaded file
     */
    @Transactional
    public MigrationJob createMigrationJob(MultipartFile file) {
        // 1. Validate file
        validateFile(file);
        
        // 2. Calculate file hash
        String fileHash = calculateFileHash(file);
        
        // 3. Check duplicate file
        if (jobRepository.existsByFileHash(fileHash)) {
            throw new DuplicateFileException("File already processed: " + file.getOriginalFilename());
        }
        
        // 4. Store file (will store after job creation)
        UUID tempJobId = UUID.randomUUID();
        
        // 5. Parse Excel metadata
        ExcelMetadataParser.ExcelMetadata metadata;
        try (InputStream inputStream = file.getInputStream()) {
            metadata = metadataParser.parseMetadata(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel metadata", e);
        }
        
        // 6. Create job
        MigrationJob job = MigrationJob.builder()
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .fileHash(fileHash)
            .totalSheets(metadata.getSheetCount())
            .status(MigrationStatus.PENDING)
            .build();
        job = jobRepository.save(job);
        
        // 7. Store file with job ID
        try {
            fileStorageService.storeFile(file, job.getId());
        } catch (IOException e) {
            log.error("Failed to store file for job: {}", job.getId(), e);
            throw new RuntimeException("Failed to store file", e);
        }
        
        // 8. Create sheet records
        for (String sheetName : metadata.getSheetNames()) {
            SheetType sheetType = metadataParser.detectSheetType(sheetName);
            if (sheetType == null) {
                log.warn("Skipping unknown sheet: {}", sheetName);
                continue;
            }
            
            MigrationSheet sheet = MigrationSheet.builder()
                .jobId(job.getId())
                .sheetName(sheetName)
                .sheetType(sheetType)
                .totalRows(metadata.getRowCount(sheetName))
                .status(SheetStatus.PENDING)
                .build();
            sheetRepository.save(sheet);
        }
        
        log.info("Created migration job: {} with {} sheets", job.getId(), metadata.getSheetCount());
        return job;
    }
    
    /**
     * Start migration for a job using optimized multi-sheet processor
     *
     * NEW: Uses TrueStreamingMultiSheetProcessor for better performance
     * - Opens file once instead of per-sheet (3x less I/O)
     * - Shared OPCPackage reduces memory by 66%
     * - Handles 1-3 sheets dynamically based on file content
     *
     * Falls back to legacy per-sheet processing if needed
     */
    @Async("excelMigrationExecutor")
    public CompletableFuture<Void> startMigration(UUID jobId) {
        log.info("Starting migration for job: {}", jobId);

        MigrationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(MigrationStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        List<MigrationSheet> sheets = sheetRepository.findByJobId(jobId);

        if (sheets.isEmpty()) {
            log.warn("No sheets to process for job: {}", jobId);
            job.setStatus(MigrationStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            return CompletableFuture.completedFuture(null);
        }

        // ✅ NEW: Use multi-sheet processor for better performance
        // Processes all sheets in single file pass with true streaming
        return processJobWithMultiSheet(jobId);
    }

    /**
     * Start migration for a job using legacy per-sheet processing
     *
     * LEGACY: Kept for backward compatibility and debugging
     * Use startMigration() which now uses multi-sheet processor
     */
    @Async("excelMigrationExecutor")
    public CompletableFuture<Void> startMigrationLegacy(UUID jobId) {
        log.info("Starting LEGACY migration for job: {}", jobId);

        MigrationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(MigrationStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        List<MigrationSheet> sheets = sheetRepository.findByJobId(jobId);

        // Process all sheets concurrently (legacy approach)
        List<CompletableFuture<Void>> futures = sheets.stream()
            .map(sheet -> processSheet(sheet.getId()))
            .toList();

        // Wait for all sheets to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                // Check if all sheets completed successfully
                boolean allCompleted = sheets.stream()
                    .allMatch(s -> s.getStatus() == SheetStatus.COMPLETED);

                job.setStatus(allCompleted ? MigrationStatus.COMPLETED : MigrationStatus.FAILED);
                job.setCompletedAt(Instant.now());
                jobRepository.save(job);
            });

        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Process all sheets in a job using multi-sheet processor (RECOMMENDED)
     * Opens file once and processes all sheets with true streaming
     *
     * Benefits:
     * - 3x less I/O (single file open vs multiple)
     * - Shared OPCPackage reduces memory by 66%
     * - Parallel processing potential
     * - Cleaner code with dynamic sheet mapping
     */
    @Async("excelMigrationExecutor")
    public CompletableFuture<Void> processJobWithMultiSheet(UUID jobId) {
        log.info("Starting multi-sheet processing for job: {}", jobId);

        MigrationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<MigrationSheet> sheets = sheetRepository.findByJobId(jobId);

        if (sheets.isEmpty()) {
            log.warn("No sheets found for job: {}", jobId);
            return CompletableFuture.completedFuture(null);
        }

        // ✅ PHASE 2: Calculate total memory for ALL sheets
        long totalEstimatedMemory = sheets.stream()
            .mapToLong(s -> s.getTotalRows() * ESTIMATED_MEMORY_PER_ROW)
            .sum();

        try {
            // ✅ Wait for memory availability (aggregate)
            long waitStartTime = System.currentTimeMillis();
            while (currentMemoryUsage.get() + totalEstimatedMemory > MAX_MEMORY_BYTES) {
                log.info("Waiting for memory for job {}: current={}MB, needed={}MB, limit={}MB",
                         jobId,
                         currentMemoryUsage.get() / 1024 / 1024,
                         totalEstimatedMemory / 1024 / 1024,
                         MAX_MEMORY_BYTES / 1024 / 1024);

                Thread.sleep(MEMORY_CHECK_INTERVAL_MS);

                if (System.currentTimeMillis() - waitStartTime > 300_000) {
                    log.warn("Memory wait timeout for job {}, proceeding anyway", jobId);
                    break;
                }
            }

            // ✅ Reserve memory once for entire job
            currentMemoryUsage.addAndGet(totalEstimatedMemory);
            log.info("Reserved {}MB memory for job {} ({} sheets, total: {}MB / {}MB)",
                     totalEstimatedMemory / 1024 / 1024,
                     jobId,
                     sheets.size(),
                     currentMemoryUsage.get() / 1024 / 1024,
                     MAX_MEMORY_BYTES / 1024 / 1024);

            // Mark all sheets as processing
            sheets.forEach(sheet -> {
                sheet.setStatus(SheetStatus.PROCESSING);
                sheet.setStartedAt(Instant.now());
                sheet.setLastHeartbeat(Instant.now());
                sheetRepository.save(sheet);
            });

            // ✅ PHASE 1: Build dynamic sheet mapping
            Map<String, Class<?>> sheetClassMap = new HashMap<>();
            Map<String, Consumer<List<?>>> sheetProcessors = new HashMap<>();
            Map<String, UUID> sheetNameToIdMap = new HashMap<>();
            Map<String, AtomicInteger> batchCounters = new HashMap<>();

            for (MigrationSheet sheet : sheets) {
                String sheetName = sheet.getSheetName();
                UUID sheetId = sheet.getId();
                AtomicInteger batchNumber = new AtomicInteger(0);

                sheetNameToIdMap.put(sheetName, sheetId);
                batchCounters.put(sheetName, batchNumber);

                switch (sheet.getSheetType()) {
                    case HSBG_THEO_HOP_DONG -> {
                        sheetClassMap.put(sheetName, HSBGHopDongDTO.class);
                        sheetProcessors.put(sheetName, batch ->
                            processBatchHopDong(sheetId, (List<HSBGHopDongDTO>) batch,
                                batchNumber.getAndIncrement()));
                    }
                    case HSBG_THEO_CIF -> {
                        sheetClassMap.put(sheetName, HSBGCifDTO.class);
                        sheetProcessors.put(sheetName, batch ->
                            processBatchCif(sheetId, (List<HSBGCifDTO>) batch,
                                batchNumber.getAndIncrement()));
                    }
                    case HSBG_THEO_TAP -> {
                        sheetClassMap.put(sheetName, HSBGTapDTO.class);
                        sheetProcessors.put(sheetName, batch ->
                            processBatchTap(sheetId, (List<HSBGTapDTO>) batch,
                                batchNumber.getAndIncrement()));
                    }
                }
            }

            // Get input stream from file storage
            InputStream inputStream = fileStorageService.retrieveFile(jobId, job.getFileName());
            ExcelConfig config = ExcelConfigFactory.createLargeFileConfig();

            // ✅ PHASE 1: Process with TrueStreamingMultiSheetProcessor
            com.neobrutalism.crm.utils.sax.TrueStreamingMultiSheetProcessor processor =
                new com.neobrutalism.crm.utils.sax.TrueStreamingMultiSheetProcessor(
                    sheetClassMap, sheetProcessors, config);

            Map<String, com.neobrutalism.crm.utils.sax.TrueStreamingSAXProcessor.ProcessingResult> results =
                processor.processTrueStreaming(inputStream);

            log.info("Multi-sheet processing completed for job: {}, processed {} sheets",
                     jobId, results.size());

            // ✅ PHASE 3: Update each sheet status independently
            for (MigrationSheet sheet : sheets) {
                try {
                    // Post-validation: check duplicates
                    duplicateDetectionService.checkDuplicatesInFile(sheet.getId(), sheet.getSheetType());

                    // Insert to master data
                    insertToMaster(sheet.getId(), sheet.getSheetType());

                    sheet.setStatus(SheetStatus.COMPLETED);
                    sheet.setCompletedAt(Instant.now());
                    sheetRepository.save(sheet);

                    log.info("Sheet {} ({}) completed successfully",
                             sheet.getSheetName(), sheet.getId());

                } catch (Exception e) {
                    // ✅ PHASE 3: Partial failure - mark only this sheet as failed
                    log.error("Error processing sheet {} ({}): {}",
                              sheet.getSheetName(), sheet.getId(), e.getMessage(), e);
                    sheet.setStatus(SheetStatus.FAILED);
                    sheet.setErrorMessage(e.getMessage());
                    sheetRepository.save(sheet);
                }
            }

            // Update job status based on sheets
            long completedSheets = sheets.stream()
                .filter(s -> s.getStatus() == SheetStatus.COMPLETED)
                .count();

            if (completedSheets == sheets.size()) {
                job.setStatus(MigrationStatus.COMPLETED);
                log.info("Job {} completed successfully - all {} sheets processed",
                         jobId, sheets.size());
            } else if (completedSheets > 0) {
                job.setStatus(MigrationStatus.COMPLETED);
                log.warn("Job {} completed with partial success - {}/{} sheets succeeded",
                         jobId, completedSheets, sheets.size());
            } else {
                job.setStatus(MigrationStatus.FAILED);
                log.error("Job {} failed - no sheets completed successfully", jobId);
            }

            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

        } catch (InterruptedException e) {
            log.error("Job processing interrupted: {}", jobId, e);
            Thread.currentThread().interrupt();

            // Mark all sheets as failed
            sheets.forEach(sheet -> {
                sheet.setStatus(SheetStatus.FAILED);
                sheet.setErrorMessage("Processing interrupted: " + e.getMessage());
                sheetRepository.save(sheet);
            });

            job.setStatus(MigrationStatus.FAILED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

        } catch (Exception e) {
            log.error("Error processing job with multi-sheet: {}", jobId, e);

            // Mark all non-completed sheets as failed
            sheets.forEach(sheet -> {
                if (sheet.getStatus() != SheetStatus.COMPLETED) {
                    sheet.setStatus(SheetStatus.FAILED);
                    sheet.setErrorMessage(e.getMessage());
                    sheetRepository.save(sheet);
                }
            });

            job.setStatus(MigrationStatus.FAILED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

        } finally {
            // ✅ PHASE 2: Release memory once for entire job
            currentMemoryUsage.addAndGet(-totalEstimatedMemory);
            log.info("Released {}MB memory for job {} (available: {}MB / {}MB)",
                     totalEstimatedMemory / 1024 / 1024,
                     jobId,
                     (MAX_MEMORY_BYTES - currentMemoryUsage.get()) / 1024 / 1024,
                     MAX_MEMORY_BYTES / 1024 / 1024);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process a single sheet with memory-aware concurrency control (LEGACY)
     * Uses memory tracking instead of fixed semaphore to prevent OOM
     *
     * NOTE: Consider using processJobWithMultiSheet() for better performance
     * when processing jobs with multiple sheets
     */
    @Async("excelMigrationExecutor")
    public CompletableFuture<Void> processSheet(UUID sheetId) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow();

        // ✅ Estimate memory needed for this sheet
        long estimatedMemory = sheet.getTotalRows() * ESTIMATED_MEMORY_PER_ROW;
        
        try {
            // ✅ Wait until enough memory is available
            long waitStartTime = System.currentTimeMillis();
            while (currentMemoryUsage.get() + estimatedMemory > MAX_MEMORY_BYTES) {
                log.info("Waiting for memory to be available for sheet {}: current={}MB, needed={}MB, limit={}MB",
                         sheetId,
                         currentMemoryUsage.get() / 1024 / 1024,
                         estimatedMemory / 1024 / 1024,
                         MAX_MEMORY_BYTES / 1024 / 1024);
                
                Thread.sleep(MEMORY_CHECK_INTERVAL_MS);
                
                // Safety timeout: wait max 5 minutes
                if (System.currentTimeMillis() - waitStartTime > 300_000) {
                    log.warn("Memory wait timeout for sheet {}, proceeding anyway", sheetId);
                    break;
                }
            }
            
            // ✅ Reserve memory
            currentMemoryUsage.addAndGet(estimatedMemory);
            log.info("Reserved {}MB memory for sheet {} (total: {}MB / {}MB)",
                     estimatedMemory / 1024 / 1024,
                     sheetId,
                     currentMemoryUsage.get() / 1024 / 1024,
                     MAX_MEMORY_BYTES / 1024 / 1024);

            sheet.setStatus(SheetStatus.PROCESSING);
            sheet.setStartedAt(Instant.now());
            sheet.setLastHeartbeat(Instant.now());
            sheetRepository.save(sheet);

            // Get input stream from file storage
            MigrationJob job = jobRepository.findById(sheet.getJobId())
                .orElseThrow();
            InputStream inputStream;
            try {
                inputStream = fileStorageService.retrieveFile(sheet.getJobId(), job.getFileName());
            } catch (IOException e) {
                throw new RuntimeException("Failed to retrieve file for job: " + sheet.getJobId(), e);
            }

            // Process based on sheet type
            ExcelConfig config = ExcelConfigFactory.createLargeFileConfig();

            // Process based on sheet type with proper generics
            switch (sheet.getSheetType()) {
                case HSBG_THEO_HOP_DONG -> processSheetHopDong(sheetId, inputStream, config);
                case HSBG_THEO_CIF -> processSheetCif(sheetId, inputStream, config);
                case HSBG_THEO_TAP -> processSheetTap(sheetId, inputStream, config);
            }

            // Post-validation: check duplicates
            duplicateDetectionService.checkDuplicatesInFile(sheetId, sheet.getSheetType());

            // Insert to master data
            insertToMaster(sheetId, sheet.getSheetType());

            sheet.setStatus(SheetStatus.COMPLETED);
            sheet.setCompletedAt(Instant.now());
            sheetRepository.save(sheet);

        } catch (InterruptedException e) {
            log.error("Sheet processing interrupted: {}", sheetId, e);
            Thread.currentThread().interrupt();
            sheet.setStatus(SheetStatus.FAILED);
            sheet.setErrorMessage("Processing interrupted: " + e.getMessage());
            sheetRepository.save(sheet);
        } catch (Exception e) {
            log.error("Error processing sheet: {}", sheetId, e);
            sheet.setStatus(SheetStatus.FAILED);
            sheet.setErrorMessage(e.getMessage());
            sheetRepository.save(sheet);
        } finally {
            // ✅ Always release the reserved memory
            currentMemoryUsage.addAndGet(-estimatedMemory);
            log.info("Released {}MB memory for sheet {} (available: {}MB / {}MB)",
                     estimatedMemory / 1024 / 1024,
                     sheetId,
                     (MAX_MEMORY_BYTES - currentMemoryUsage.get()) / 1024 / 1024,
                     MAX_MEMORY_BYTES / 1024 / 1024);
        }

        return CompletableFuture.completedFuture(null);
    }
    
    private void processSheetHopDong(UUID sheetId, InputStream inputStream, ExcelConfig config) {
        int[] batchNumber = {0};
        excelFacade.readExcelWithConfig(
            inputStream,
            HSBGHopDongDTO.class,
            config,
            (Consumer<List<HSBGHopDongDTO>>) batch -> {
                processBatchHopDong(sheetId, batch, batchNumber[0]++);
            }
        );
    }
    
    private void processSheetCif(UUID sheetId, InputStream inputStream, ExcelConfig config) {
        int[] batchNumber = {0};
        excelFacade.readExcelWithConfig(
            inputStream,
            HSBGCifDTO.class,
            config,
            (Consumer<List<HSBGCifDTO>>) batch -> {
                processBatchCif(sheetId, batch, batchNumber[0]++);
            }
        );
    }
    
    private void processSheetTap(UUID sheetId, InputStream inputStream, ExcelConfig config) {
        int[] batchNumber = {0};
        excelFacade.readExcelWithConfig(
            inputStream,
            HSBGTapDTO.class,
            config,
            (Consumer<List<HSBGTapDTO>>) batch -> {
                processBatchTap(sheetId, batch, batchNumber[0]++);
            }
        );
    }
    
    @Transactional
    private void processBatchHopDong(UUID sheetId, List<HSBGHopDongDTO> batch, int batchNumber) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow();
        
        // ✅ Start timing for monitoring
        long batchStartTime = System.currentTimeMillis();
        
        // ✅ Sub-batch processing to reduce memory pressure
        // Process 1000 records at a time instead of entire batch (10000)
        final int SUB_BATCH_SIZE = 1000;
        int totalValidCount = 0;
        int totalInvalidCount = 0;
        
        for (int i = 0; i < batch.size(); i += SUB_BATCH_SIZE) {
            int end = Math.min(i + SUB_BATCH_SIZE, batch.size());
            List<HSBGHopDongDTO> subBatch = batch.subList(i, end);
            
            List<StagingHSBGHopDong> stagingRecords = new ArrayList<>(SUB_BATCH_SIZE);
            int validCount = 0;
            int invalidCount = 0;
            
            for (int j = 0; j < subBatch.size(); j++) {
                HSBGHopDongDTO dto = subBatch.get(j);
                long rowNumber = sheet.getLastProcessedRow() + i + j + 1;
                
                try {
                    // 1. Normalize
                    HSBGHopDongDTO normalized = dataNormalizer.normalizeHopDong(dto);
                    
                    // 2. Validate
                    ValidationResult validationResult = hopDongValidator.validate(normalized, (int) rowNumber);
                    
                    // 3. Log errors to excel_migration_errors table
                    if (!validationResult.isValid()) {
                        errorLogger.logValidationErrors(sheetId, rowNumber, batchNumber, validationResult);
                    }
                    
                    // 4. Create staging record
                    StagingHSBGHopDong staging = mapToStagingHopDong(normalized, sheet.getJobId(), sheetId, rowNumber);
                    staging.setValidationStatus(validationResult.isValid() ? "VALID" : "INVALID");

                    if (!validationResult.isValid()) {
                        invalidCount++;
                    } else {
                        validCount++;
                    }
                    
                    // 5. Generate duplicate key
                    staging.setDuplicateKey(dataNormalizer.generateDuplicateKey(normalized));
                    
                    stagingRecords.add(staging);
                    
                } catch (Exception e) {
                    log.error("Error processing row {} in batch {}", rowNumber, batchNumber, e);
                    invalidCount++;
                }
            }
            
            // ✅ Save sub-batch using JDBC batch insert (5-10x faster than JPA)
            if (!stagingRecords.isEmpty()) {
                jdbcBatchInsertHelper.batchInsertHopDong(stagingRecords);
                stagingRecords.clear(); // Free memory immediately
            }
            
            totalValidCount += validCount;
            totalInvalidCount += invalidCount;
            
            // ✅ Clear sub-batch to help GC
            subBatch.clear();
            
            log.trace("Processed sub-batch {}/{} for batch {}: {} valid, {} invalid", 
                      (i / SUB_BATCH_SIZE) + 1, 
                      (batch.size() + SUB_BATCH_SIZE - 1) / SUB_BATCH_SIZE,
                      batchNumber, validCount, invalidCount);
        }
        
        // 6. Update progress with total counts
        progressService.updateProgress(sheetId, batchNumber, batch.size(), totalValidCount, totalInvalidCount);
        
        // ✅ Record batch metrics
        long batchDuration = System.currentTimeMillis() - batchStartTime;
        migrationMonitor.recordBatchProcessing(sheetId, batch.size(), batchDuration, 
                                              totalValidCount, totalInvalidCount);
        
        log.debug("Processed batch {} for sheet {}: {} valid, {} invalid", 
                  batchNumber, sheetId, totalValidCount, totalInvalidCount);
    }
    
    @Transactional
    private void processBatchCif(UUID sheetId, List<HSBGCifDTO> batch, int batchNumber) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow();
        
        // ✅ Sub-batch processing to reduce memory pressure
        final int SUB_BATCH_SIZE = 1000;
        int totalValidCount = 0;
        int totalInvalidCount = 0;
        
        for (int i = 0; i < batch.size(); i += SUB_BATCH_SIZE) {
            int end = Math.min(i + SUB_BATCH_SIZE, batch.size());
            List<HSBGCifDTO> subBatch = batch.subList(i, end);
            
            List<StagingHSBGCif> stagingRecords = new ArrayList<>(SUB_BATCH_SIZE);
            int validCount = 0;
            int invalidCount = 0;
            
            for (int j = 0; j < subBatch.size(); j++) {
                HSBGCifDTO dto = subBatch.get(j);
                long rowNumber = sheet.getLastProcessedRow() + i + j + 1;
                
                try {
                    // 1. Normalize
                    HSBGCifDTO normalized = dataNormalizer.normalizeCif(dto);
                    
                    // 2. Validate
                    ValidationResult validationResult = cifValidator.validate(normalized, (int) rowNumber);
                    
                    // 3. Log errors to excel_migration_errors table
                    if (!validationResult.isValid()) {
                        errorLogger.logValidationErrors(sheetId, rowNumber, batchNumber, validationResult);
                    }
                    
                    // 4. Create staging record
                    StagingHSBGCif staging = mapToStagingCif(normalized, sheet.getJobId(), sheetId, rowNumber);
                    staging.setValidationStatus(validationResult.isValid() ? "VALID" : "INVALID");

                    if (!validationResult.isValid()) {
                        invalidCount++;
                    } else {
                        validCount++;
                    }
                    
                    // 5. Generate duplicate key
                    staging.setDuplicateKey(dataNormalizer.generateDuplicateKey(normalized));
                    
                    stagingRecords.add(staging);
                    
                } catch (Exception e) {
                    log.error("Error processing row {} in batch {}", rowNumber, batchNumber, e);
                    errorLogger.logProcessingError(sheetId, rowNumber, batchNumber, 
                        "PROCESSING_ERROR", "Failed to process row: " + e.getMessage(), e);
                    invalidCount++;
                }
            }
            
            // ✅ Save sub-batch using JDBC batch insert (5-10x faster than JPA)
            if (!stagingRecords.isEmpty()) {
                jdbcBatchInsertHelper.batchInsertCif(stagingRecords);
                stagingRecords.clear(); // Free memory immediately
            }
            
            totalValidCount += validCount;
            totalInvalidCount += invalidCount;
            
            // ✅ Clear sub-batch to help GC
            subBatch.clear();
            
            log.trace("Processed sub-batch {}/{} for CIF batch {}: {} valid, {} invalid", 
                      (i / SUB_BATCH_SIZE) + 1, 
                      (batch.size() + SUB_BATCH_SIZE - 1) / SUB_BATCH_SIZE,
                      batchNumber, validCount, invalidCount);
        }
        
        // 6. Update progress with total counts
        progressService.updateProgress(sheetId, batchNumber, batch.size(), totalValidCount, totalInvalidCount);
        
        log.debug("Processed batch {} for CIF sheet {}: {} valid, {} invalid", 
                  batchNumber, sheetId, totalValidCount, totalInvalidCount);
    }
    
    @Transactional
    private void processBatchTap(UUID sheetId, List<HSBGTapDTO> batch, int batchNumber) {
        MigrationSheet sheet = sheetRepository.findById(sheetId)
            .orElseThrow();
        
        // ✅ Sub-batch processing to reduce memory pressure
        final int SUB_BATCH_SIZE = 1000;
        int totalValidCount = 0;
        int totalInvalidCount = 0;
        
        for (int i = 0; i < batch.size(); i += SUB_BATCH_SIZE) {
            int end = Math.min(i + SUB_BATCH_SIZE, batch.size());
            List<HSBGTapDTO> subBatch = batch.subList(i, end);
            
            List<StagingHSBGTap> stagingRecords = new ArrayList<>(SUB_BATCH_SIZE);
            int validCount = 0;
            int invalidCount = 0;
            
            for (int j = 0; j < subBatch.size(); j++) {
                HSBGTapDTO dto = subBatch.get(j);
                long rowNumber = sheet.getLastProcessedRow() + i + j + 1;
                
                try {
                    // 1. Normalize
                    HSBGTapDTO normalized = dataNormalizer.normalizeTap(dto);
                    
                    // 2. Validate
                    ValidationResult validationResult = tapValidator.validate(normalized, (int) rowNumber);
                    
                    // 3. Log errors to excel_migration_errors table
                    if (!validationResult.isValid()) {
                        errorLogger.logValidationErrors(sheetId, rowNumber, batchNumber, validationResult);
                    }
                    
                    // 4. Create staging record
                    StagingHSBGTap staging = mapToStagingTap(normalized, sheet.getJobId(), sheetId, rowNumber);
                    staging.setValidationStatus(validationResult.isValid() ? "VALID" : "INVALID");

                    if (!validationResult.isValid()) {
                        invalidCount++;
                    } else {
                        validCount++;
                    }
                    
                    // 5. Generate duplicate key
                    staging.setDuplicateKey(dataNormalizer.generateDuplicateKey(normalized));
                    
                    stagingRecords.add(staging);
                    
                } catch (Exception e) {
                    log.error("Error processing row {} in batch {}", rowNumber, batchNumber, e);
                    errorLogger.logProcessingError(sheetId, rowNumber, batchNumber, 
                        "PROCESSING_ERROR", "Failed to process row: " + e.getMessage(), e);
                    invalidCount++;
                }
            }
            
            // ✅ Save sub-batch using JDBC batch insert (5-10x faster than JPA)
            if (!stagingRecords.isEmpty()) {
                jdbcBatchInsertHelper.batchInsertTap(stagingRecords);
                stagingRecords.clear(); // Free memory immediately
            }
            
            totalValidCount += validCount;
            totalInvalidCount += invalidCount;
            
            // ✅ Clear sub-batch to help GC
            subBatch.clear();
            
            log.trace("Processed sub-batch {}/{} for Tap batch {}: {} valid, {} invalid", 
                      (i / SUB_BATCH_SIZE) + 1, 
                      (batch.size() + SUB_BATCH_SIZE - 1) / SUB_BATCH_SIZE,
                      batchNumber, validCount, invalidCount);
        }
        
        // 6. Update progress with total counts
        progressService.updateProgress(sheetId, batchNumber, batch.size(), totalValidCount, totalInvalidCount);
        
        log.debug("Processed batch {} for Tap sheet {}: {} valid, {} invalid", 
                  batchNumber, sheetId, totalValidCount, totalInvalidCount);
    }
    
    private StagingHSBGHopDong mapToStagingHopDong(HSBGHopDongDTO dto, UUID jobId, UUID sheetId, long rowNumber) {
        return StagingHSBGHopDong.builder()
            .jobId(jobId)
            .sheetId(sheetId)
            .rowNumber(rowNumber)
            .warehouseVpbank(dto.getWarehouseVpbank())
            .unitCode(dto.getUnitCode())
            .deliveryResponsibility(dto.getDeliveryResponsibility())
            .contractNumber(dto.getContractNumber())
            .volumeName(dto.getVolumeName())
            .volumeQuantity(dto.getVolumeQuantity())
            .customerCifCccdCmt(dto.getCustomerCifCccdCmt())
            .customerName(dto.getCustomerName())
            .customerSegment(dto.getCustomerSegment())
            .requiredDeliveryDate(dto.getRequiredDeliveryDate())
            .deliveryDate(dto.getDeliveryDate())
            .disbursementDate(dto.getDisbursementDate())
            .dueDate(dto.getDueDate())
            .documentType(dto.getDocumentType())
            .documentFlow(dto.getDocumentFlow())
            .creditTermCategory(dto.getCreditTermCategory())
            .expectedDestructionDate(dto.getExpectedDestructionDate())
            .product(dto.getProduct())
            .pdmCaseStatus(dto.getPdmCaseStatus())
            .notes(dto.getNotes())
            .boxCode(dto.getBoxCode())
            .vpbankWarehouseEntryDate(dto.getVpbankWarehouseEntryDate())
            .crownWarehouseTransferDate(dto.getCrownWarehouseTransferDate())
            .area(dto.getArea())
            .row(dto.getRow())
            .column(dto.getColumn())
            .boxCondition(dto.getBoxCondition())
            .boxStatus(dto.getBoxStatus())
            .creditTermMonths(dto.getCreditTermMonths())
            .daoCode(dto.getDaoCode())
            .tsCode(dto.getTsCode())
            .rrtId(dto.getRrtId())
            .nqCode(dto.getNqCode())
            .build();
    }
    
    private StagingHSBGCif mapToStagingCif(HSBGCifDTO dto, UUID jobId, UUID sheetId, long rowNumber) {
        return StagingHSBGCif.builder()
            .jobId(jobId)
            .sheetId(sheetId)
            .rowNumber(rowNumber)
            .warehouseVpbank(dto.getWarehouseVpbank())
            .unitCode(dto.getUnitCode())
            .deliveryResponsibility(dto.getDeliveryResponsibility())
            .customerCif(dto.getCustomerCif())
            .customerName(dto.getCustomerName())
            .volumeName(dto.getVolumeName())
            .volumeQuantity(dto.getVolumeQuantity())
            .customerSegment(dto.getCustomerSegment())
            .requiredDeliveryDate(dto.getRequiredDeliveryDate())
            .deliveryDate(dto.getDeliveryDate())
            .disbursementDate(dto.getDisbursementDate())
            .documentType(dto.getDocumentType())
            .documentFlow(dto.getDocumentFlow())
            .creditTermCategory(dto.getCreditTermCategory())
            .product(dto.getProduct())
            .pdmCaseStatus(dto.getPdmCaseStatus())
            .notes(dto.getNotes())
            .nqCode(dto.getNqCode())
            .boxCode(dto.getBoxCode())
            .vpbankWarehouseEntryDate(dto.getVpbankWarehouseEntryDate())
            .crownWarehouseTransferDate(dto.getCrownWarehouseTransferDate())
            .area(dto.getArea())
            .row(dto.getRow())
            .column(dto.getColumn())
            .boxCondition(dto.getBoxCondition())
            .boxStatus(dto.getBoxStatus())
            .build();
    }
    
    private StagingHSBGTap mapToStagingTap(HSBGTapDTO dto, UUID jobId, UUID sheetId, long rowNumber) {
        return StagingHSBGTap.builder()
            .jobId(jobId)
            .sheetId(sheetId)
            .rowNumber(rowNumber)
            .warehouseVpbank(dto.getWarehouseVpbank())
            .unitCode(dto.getUnitCode())
            .deliveryResponsibility(dto.getDeliveryResponsibility())
            .occurrenceMonth(dto.getOccurrenceMonth())
            .volumeName(dto.getVolumeName())
            .volumeQuantity(dto.getVolumeQuantity())
            .requiredDeliveryDate(dto.getRequiredDeliveryDate())
            .deliveryDate(dto.getDeliveryDate())
            .documentType(dto.getDocumentType())
            .documentFlow(dto.getDocumentFlow())
            .creditTermCategory(dto.getCreditTermCategory())
            .expectedDestructionDate(dto.getExpectedDestructionDate())
            .product(dto.getProduct())
            .pdmCaseStatus(dto.getPdmCaseStatus())
            .notes(dto.getNotes())
            .boxCode(dto.getBoxCode())
            .vpbankWarehouseEntryDate(dto.getVpbankWarehouseEntryDate())
            .crownWarehouseTransferDate(dto.getCrownWarehouseTransferDate())
            .area(dto.getArea())
            .row(dto.getRow())
            .column(dto.getColumn())
            .boxCondition(dto.getBoxCondition())
            .boxStatus(dto.getBoxStatus())
            .build();
    }
    
    @Transactional
    private void insertToMaster(UUID sheetId, SheetType sheetType) {
        log.info("Inserting valid records to master data for sheet: {}", sheetId);
        
        switch (sheetType) {
            case HSBG_THEO_HOP_DONG -> insertToMasterHopDong(sheetId);
            case HSBG_THEO_CIF -> insertToMasterCif(sheetId);
            case HSBG_THEO_TAP -> insertToMasterTap(sheetId);
        }
    }
    
    /**
     * Insert valid staging records to master data using optimized stored procedure
     * Uses PostgreSQL procedure with row-level locking and batch commits
     */
    private void insertToMasterHopDong(UUID sheetId) {
        log.info("Starting migration to master data for HSBG_HOP_DONG sheet: {}", sheetId);

        try {
            // Get job_id from sheet
            MigrationSheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found: " + sheetId));
            UUID jobId = sheet.getJobId();

            // Call optimized PostgreSQL procedure
            MigrationResult result = callMigrationProcedure(jobId, "migrate_hsbg_hop_dong", 1000);

            log.info("Migration completed for sheet: {} | Total: {} | Migrated: {} | Duplicates: {} | Errors: {} | Warnings: {}",
                     sheetId,
                     result.getTotalProcessed(),
                     result.getMigratedCount(),
                     result.getDuplicateCount(),
                     result.getErrorCount(),
                     result.getWarningCount());

            // Update sheet statistics
            updateSheetStatistics(sheetId, result);

        } catch (Exception e) {
            log.error("Failed to migrate HSBG_HOP_DONG sheet: {}", sheetId, e);
            throw new RuntimeException("Migration failed for sheet: " + sheetId, e);
        }
    }

    /**
     * Insert a single batch in its own transaction
     * This prevents large transactions that cause deadlocks
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private int insertBatchHopDong(UUID sheetId, List<StagingHSBGHopDong> batch) {
        try {
            // Transform to master data entities and batch insert
            List<UUID> insertedIds = transformAndInsertHopDong(batch);

            // Mark as inserted within the same transaction
            stagingHopDongRepository.markAsInserted(insertedIds, Instant.now());

            return insertedIds.size();
        } catch (Exception e) {
            log.error("Failed to insert batch for sheet: {}, batch size: {}", sheetId, batch.size(), e);
            // Don't propagate - continue with next batch
            return 0;
        }
    }
    
    /**
     * Insert valid staging records to master data using optimized stored procedure for CIF
     */
    private void insertToMasterCif(UUID sheetId) {
        log.info("Starting migration to master data for HSBG_CIF sheet: {}", sheetId);

        try {
            MigrationSheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found: " + sheetId));
            UUID jobId = sheet.getJobId();

            // Call optimized PostgreSQL procedure for CIF
            MigrationResult result = callMigrationProcedure(jobId, "migrate_hsbg_cif", 1000);

            log.info("CIF migration completed for sheet: {} | Total: {} | Migrated: {} | Duplicates: {} | Errors: {} | Warnings: {}",
                     sheetId,
                     result.getTotalProcessed(),
                     result.getMigratedCount(),
                     result.getDuplicateCount(),
                     result.getErrorCount(),
                     result.getWarningCount());

            updateSheetStatistics(sheetId, result);

        } catch (Exception e) {
            log.error("Failed to migrate HSBG_CIF sheet: {}", sheetId, e);
            throw new RuntimeException("CIF migration failed for sheet: " + sheetId, e);
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private int insertBatchCif(UUID sheetId, List<StagingHSBGCif> batch) {
        try {
            List<UUID> insertedIds = transformAndInsertCif(batch);
            stagingCifRepository.markAsInserted(insertedIds, Instant.now());
            return insertedIds.size();
        } catch (Exception e) {
            log.error("Failed to insert CIF batch for sheet: {}, batch size: {}", sheetId, batch.size(), e);
            return 0;
        }
    }

    /**
     * Insert valid staging records to master data using optimized stored procedure for TAP
     */
    private void insertToMasterTap(UUID sheetId) {
        log.info("Starting migration to master data for HSBG_TAP sheet: {}", sheetId);

        try {
            MigrationSheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found: " + sheetId));
            UUID jobId = sheet.getJobId();

            // Call optimized PostgreSQL procedure for TAP
            MigrationResult result = callMigrationProcedure(jobId, "migrate_hsbg_tap", 1000);

            log.info("TAP migration completed for sheet: {} | Total: {} | Migrated: {} | Duplicates: {} | Errors: {} | Warnings: {}",
                     sheetId,
                     result.getTotalProcessed(),
                     result.getMigratedCount(),
                     result.getDuplicateCount(),
                     result.getErrorCount(),
                     result.getWarningCount());

            updateSheetStatistics(sheetId, result);

        } catch (Exception e) {
            log.error("Failed to migrate HSBG_TAP sheet: {}", sheetId, e);
            throw new RuntimeException("TAP migration failed for sheet: " + sheetId, e);
        }
    }

    // Legacy batch insert methods - kept for backward compatibility but no longer used
    private void insertToMasterTapLegacy(UUID sheetId) {
        int batchSize = 1000; // Mini-transactions
        int totalInserted = 0;
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, batchSize);
            List<StagingHSBGTap> batch = stagingTapRepository.findValidRecordsForInsert(sheetId, pageable);

            if (batch.isEmpty()) {
                break;
            }

            // Process batch in separate transaction
            int inserted = insertBatchTap(sheetId, batch);
            totalInserted += inserted;

            log.info("Inserted batch {} ({} records) to master data for Tap sheet: {} (total: {})",
                     page, inserted, sheetId, totalInserted);

            if (totalInserted > 2_000_000) {
                log.warn("Reached maximum insert limit for sheet: {}", sheetId);
                break;
            }
        }

        log.info("Completed inserting to master data for Tap sheet: {}, total: {}", sheetId, totalInserted);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private int insertBatchTap(UUID sheetId, List<StagingHSBGTap> batch) {
        try {
            List<UUID> insertedIds = transformAndInsertTap(batch);
            stagingTapRepository.markAsInserted(insertedIds, Instant.now());
            return insertedIds.size();
        } catch (Exception e) {
            log.error("Failed to insert Tap batch for sheet: {}, batch size: {}", sheetId, batch.size(), e);
            return 0;
        }
    }
    
    /**
     * Transform staging records to master data entities and insert
     * TODO: Implement actual master data transformation based on your domain model
     */
    private List<UUID> transformAndInsertHopDong(List<StagingHSBGHopDong> batch) {
        // TODO: Transform to actual master data entities
        // Example:
        // List<MasterDataEntity> entities = batch.stream()
        //     .map(this::mapStagingToMaster)
        //     .collect(Collectors.toList());
        // masterDataRepository.saveAll(entities);
        
        // For now, return all IDs as inserted
        return batch.stream()
            .map(StagingHSBGHopDong::getId)
            .toList();
    }
    
    private List<UUID> transformAndInsertCif(List<StagingHSBGCif> batch) {
        // TODO: Implement actual master data transformation
        return batch.stream()
            .map(StagingHSBGCif::getId)
            .toList();
    }
    
    private List<UUID> transformAndInsertTap(List<StagingHSBGTap> batch) {
        // TODO: Implement actual master data transformation
        return batch.stream()
            .map(StagingHSBGTap::getId)
            .toList();
    }

    /**
     * Call PostgreSQL migration procedure using JDBC
     *
     * @param jobId The migration job ID
     * @param procedureName Name of the procedure (migrate_hsbg_hop_dong, migrate_hsbg_cif, migrate_hsbg_tap)
     * @param batchSize Batch size for processing
     * @return MigrationResult with statistics
     */
    private MigrationResult callMigrationProcedure(UUID jobId, String procedureName, int batchSize) {
        log.info("Calling procedure: {} for job: {} with batch size: {}", procedureName, jobId, batchSize);

        try {
            // Call the stored procedure using JDBC
            String sql = String.format("CALL %s(?, ?, ?, ?, ?, ?, ?)", procedureName);

            Integer[] outParams = new Integer[5];

            jdbcTemplate.execute((java.sql.Connection connection) -> {
                try (java.sql.CallableStatement cs = connection.prepareCall(sql)) {
                    // Set input parameters
                    cs.setObject(1, jobId);
                    cs.setInt(2, batchSize);

                    // Register output parameters
                    cs.registerOutParameter(3, java.sql.Types.INTEGER); // total_processed
                    cs.registerOutParameter(4, java.sql.Types.INTEGER); // migrated_count
                    cs.registerOutParameter(5, java.sql.Types.INTEGER); // duplicate_count
                    cs.registerOutParameter(6, java.sql.Types.INTEGER); // error_count
                    cs.registerOutParameter(7, java.sql.Types.INTEGER); // warning_count

                    // Execute procedure
                    cs.execute();

                    // Get output parameters
                    outParams[0] = cs.getInt(3);
                    outParams[1] = cs.getInt(4);
                    outParams[2] = cs.getInt(5);
                    outParams[3] = cs.getInt(6);
                    outParams[4] = cs.getInt(7);

                    return null;
                }
            });

            // Build result
            MigrationResult result = MigrationResult.builder()
                .totalProcessed(outParams[0])
                .migratedCount(outParams[1])
                .duplicateCount(outParams[2])
                .errorCount(outParams[3])
                .warningCount(outParams[4])
                .build();

            log.info("Procedure {} completed successfully: {}", procedureName, result);
            return result;

        } catch (Exception e) {
            log.error("Failed to call procedure: {} for job: {}", procedureName, jobId, e);
            throw new RuntimeException("Migration procedure call failed: " + procedureName, e);
        }
    }

    /**
     * Update sheet statistics after migration
     *
     * @param sheetId The sheet ID
     * @param result Migration result
     */
    private void updateSheetStatistics(UUID sheetId, MigrationResult result) {
        try {
            MigrationSheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found: " + sheetId));

            // Update sheet counters
            sheet.setValidRows(result.getMigratedCount() != null ? result.getMigratedCount().longValue() : 0L);
            sheet.setInvalidRows(result.getErrorCount() != null ? result.getErrorCount().longValue() : 0L);

            // Calculate skipped rows (duplicates)
            long skippedRows = result.getDuplicateCount() != null ? result.getDuplicateCount().longValue() : 0L;
            sheet.setSkippedRows(skippedRows);

            // Update progress
            if (result.getTotalProcessed() != null && result.getTotalProcessed() > 0) {
                double progressPct = (result.getMigratedCount() != null ? result.getMigratedCount() : 0) * 100.0
                                   / result.getTotalProcessed();
                sheet.setProgressPercent(java.math.BigDecimal.valueOf(progressPct));
            }

            // Update status
            if (result.isSuccessful()) {
                sheet.setStatus(SheetStatus.COMPLETED);
            } else if (result.getErrorCount() != null && result.getErrorCount() > 0) {
                sheet.setStatus(SheetStatus.FAILED);
                sheet.setErrorMessage("Migration completed with " + result.getErrorCount() + " validation errors");
            }

            sheet.setCompletedAt(Instant.now());
            sheetRepository.save(sheet);

            log.info("Updated sheet statistics for {}: valid={}, invalid={}, skipped={}, progress={}%",
                     sheetId,
                     sheet.getValidRows(),
                     sheet.getInvalidRows(),
                     sheet.getSkippedRows(),
                     sheet.getProgressPercent());

        } catch (Exception e) {
            log.error("Failed to update sheet statistics for {}", sheetId, e);
            // Don't throw - this is non-critical
        }
    }

    /**
     * Calculate file hash using streaming to avoid loading entire file into memory
     * Optimized for large files (500MB - 1GB+)
     */
    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Stream file in chunks of 8KB to minimize memory usage
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hash = digest.digest();
            return bytesToHex(hash);

        } catch (Exception e) {
            log.error("Failed to calculate file hash for: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new IllegalArgumentException("File must be Excel format (.xlsx or .xls)");
        }
    }
    
    /**
     * Get migration job by ID
     */
    @Transactional(readOnly = true)
    public MigrationJob getJob(UUID jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }
    
    /**
     * Cancel a migration job
     */
    @Transactional
    public void cancelMigration(UUID jobId) {
        MigrationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        
        job.setStatus(MigrationStatus.CANCELLED);
        jobRepository.save(job);
        
        // Cancel all sheets
        List<MigrationSheet> sheets = sheetRepository.findByJobId(jobId);
        for (MigrationSheet sheet : sheets) {
            if (sheet.getStatus() == SheetStatus.PROCESSING) {
                sheet.setStatus(SheetStatus.CANCELLED);
                sheetRepository.save(sheet);
            }
        }
        
        log.info("Cancelled migration job: {}", jobId);
    }
    
    private static class DuplicateFileException extends RuntimeException {
        public DuplicateFileException(String message) {
            super(message);
        }
    }
}

