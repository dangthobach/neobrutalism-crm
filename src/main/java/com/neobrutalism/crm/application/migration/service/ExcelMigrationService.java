package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.excel.ExcelFacade;
import com.neobrutalism.crm.application.migration.dto.HSBGCifDTO;
import com.neobrutalism.crm.application.migration.dto.HSBGHopDongDTO;
import com.neobrutalism.crm.application.migration.dto.HSBGTapDTO;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    private final FileStorageService fileStorageService;
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
     * Start migration for a job
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
        
        // Process all sheets concurrently
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
     * Process a single sheet with memory-aware concurrency control
     * Uses memory tracking instead of fixed semaphore to prevent OOM
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
                        staging.setValidationErrors(objectMapper.writeValueAsString(validationResult.getErrors()));
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
                        staging.setValidationErrors(objectMapper.writeValueAsString(validationResult.getErrors()));
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
                        staging.setValidationErrors(objectMapper.writeValueAsString(validationResult.getErrors()));
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
     * Insert valid staging records to master data with pagination
     * Prevents OOM by processing in batches of 5000 records
     * Each batch has its own mini-transaction to prevent deadlock
     */
    private void insertToMasterHopDong(UUID sheetId) {
        int batchSize = 1000; // Reduced from 5000 to 1000 for faster commits
        int totalInserted = 0;
        int page = 0;

        while (true) {
            // Use Pageable to limit query results and prevent OOM
            Pageable pageable = PageRequest.of(page, batchSize);
            List<StagingHSBGHopDong> batch = stagingHopDongRepository.findValidRecordsForInsert(sheetId, pageable);

            if (batch.isEmpty()) {
                break;
            }

            // Process batch in separate transaction to avoid large locks
            int inserted = insertBatchHopDong(sheetId, batch);
            totalInserted += inserted;

            log.info("Inserted batch {} ({} records) to master data for sheet: {} (total: {})",
                     page, inserted, sheetId, totalInserted);

            // Since we're marking as inserted, always query page 0 to get next unmarked batch
            // Don't increment page - always query first page of unmarked records

            // Safety check to avoid infinite loop
            if (totalInserted > 2_000_000) {
                log.warn("Reached maximum insert limit for sheet: {}", sheetId);
                break;
            }
        }

        log.info("Completed inserting to master data for sheet: {}, total: {}", sheetId, totalInserted);
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
            stagingHopDongRepository.markAsInserted(insertedIds);

            return insertedIds.size();
        } catch (Exception e) {
            log.error("Failed to insert batch for sheet: {}, batch size: {}", sheetId, batch.size(), e);
            // Don't propagate - continue with next batch
            return 0;
        }
    }
    
    private void insertToMasterCif(UUID sheetId) {
        int batchSize = 1000; // Mini-transactions
        int totalInserted = 0;
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, batchSize);
            List<StagingHSBGCif> batch = stagingCifRepository.findValidRecordsForInsert(sheetId, pageable);

            if (batch.isEmpty()) {
                break;
            }

            // Process batch in separate transaction
            int inserted = insertBatchCif(sheetId, batch);
            totalInserted += inserted;

            log.info("Inserted batch {} ({} records) to master data for CIF sheet: {} (total: {})",
                     page, inserted, sheetId, totalInserted);

            if (totalInserted > 2_000_000) {
                log.warn("Reached maximum insert limit for sheet: {}", sheetId);
                break;
            }
        }

        log.info("Completed inserting to master data for CIF sheet: {}, total: {}", sheetId, totalInserted);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private int insertBatchCif(UUID sheetId, List<StagingHSBGCif> batch) {
        try {
            List<UUID> insertedIds = transformAndInsertCif(batch);
            stagingCifRepository.markAsInserted(insertedIds);
            return insertedIds.size();
        } catch (Exception e) {
            log.error("Failed to insert CIF batch for sheet: {}, batch size: {}", sheetId, batch.size(), e);
            return 0;
        }
    }

    private void insertToMasterTap(UUID sheetId) {
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
            stagingTapRepository.markAsInserted(insertedIds);
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

