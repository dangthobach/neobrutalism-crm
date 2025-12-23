# 📋 Kế Hoạch Triển Khai Migration Excel - High Performance Data Migration

**Ngày**: 2025-11-09  
**Mục tiêu**: Migration data từ Excel files (~200k records/file, 1-3 sheets) vào master data tables với:
- ✅ High performance (tốc độ cao)
- ✅ Data accuracy (chính xác 100%)
- ✅ No data garbage (không có dữ liệu rác)
- ✅ No table locking (không lock database)
- ✅ Real-time progress monitoring (theo dõi tiến độ realtime)
- ✅ Stuck detection & recovery
- ✅ Exception handling & retry

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Excel Migration Service                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ File Upload  │───▶│ File Parser  │───▶│ Validator    │ │
│  │   Service    │    │   Service    │    │   Service    │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Migration Orchestrator (Coordinator)          │   │
│  │  - Progress Tracking                                  │   │
│  │  - Stuck Detection                                    │   │
│  │  - Error Recovery                                     │   │
│  │  - Retry Logic                                        │   │
│  └──────────────────────────────────────────────────────┘   │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Data         │    │ Master Data  │    │ Progress     │ │
│  │ Normalizer   │    │ Writer       │    │ Monitor      │ │
│  │              │    │ (Batch)      │    │ (Real-time)  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         └────────────────────┼────────────────────┘         │
│                              ▼                               │
│                    ┌──────────────────┐                    │
│                    │   Database        │                    │
│                    │   (No Locking)    │                    │
│                    └──────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Database Schema Design

### 1. Migration Tracking Table

```sql
CREATE TABLE excel_migration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(64) NOT NULL, -- SHA-256 for duplicate detection
    total_sheets INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    error_stack_trace TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    
    -- Indexes
    INDEX idx_migration_status (status),
    INDEX idx_migration_created_at (created_at),
    INDEX idx_migration_file_hash (file_hash)
);

CREATE TABLE excel_migration_sheets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES excel_migration_jobs(id) ON DELETE CASCADE,
    sheet_name VARCHAR(100) NOT NULL,
    sheet_type VARCHAR(50) NOT NULL, -- HSBG_THEO_HOP_DONG, HSBG_THEO_CIF, HSBG_THEO_TAP
    total_rows BIGINT NOT NULL,
    processed_rows BIGINT NOT NULL DEFAULT 0,
    valid_rows BIGINT NOT NULL DEFAULT 0,
    invalid_rows BIGINT NOT NULL DEFAULT 0,
    skipped_rows BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    last_processed_row BIGINT NOT NULL DEFAULT 0, -- For recovery
    last_heartbeat TIMESTAMP, -- For stuck detection
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_sheet_job_id (job_id),
    INDEX idx_sheet_status (status),
    INDEX idx_sheet_heartbeat (last_heartbeat),
    UNIQUE KEY uk_sheet_job_name (job_id, sheet_name)
);

CREATE TABLE excel_migration_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sheet_id UUID NOT NULL REFERENCES excel_migration_sheets(id) ON DELETE CASCADE,
    batch_number INTEGER NOT NULL,
    batch_size INTEGER NOT NULL,
    processed_count INTEGER NOT NULL,
    valid_count INTEGER NOT NULL,
    invalid_count INTEGER NOT NULL,
    skipped_count INTEGER NOT NULL,
    processing_time_ms BIGINT,
    status VARCHAR(50) NOT NULL, -- PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Indexes
    INDEX idx_progress_sheet_id (sheet_id),
    INDEX idx_progress_batch (sheet_id, batch_number)
);

CREATE TABLE excel_migration_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sheet_id UUID NOT NULL REFERENCES excel_migration_sheets(id) ON DELETE CASCADE,
    row_number BIGINT NOT NULL,
    batch_number INTEGER,
    error_code VARCHAR(50) NOT NULL,
    error_message TEXT NOT NULL,
    error_data JSONB, -- Store row data for debugging
    validation_rule VARCHAR(100), -- Which validation rule failed
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_error_sheet_id (sheet_id),
    INDEX idx_error_code (error_code),
    INDEX idx_error_row (sheet_id, row_number)
);
```

### 2. Staging Tables (No Locking Strategy)

```sql
-- Staging table for HSBG_theo_hop_dong
CREATE TABLE staging_hsbg_hop_dong (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    sheet_id UUID NOT NULL,
    row_number BIGINT NOT NULL,
    
    -- Data fields (all columns from Excel)
    kho_vpbank VARCHAR(255),
    ma_don_vi VARCHAR(100),
    trach_nhiem_ban_giao VARCHAR(255),
    so_hop_dong VARCHAR(100),
    ten_tap VARCHAR(255),
    so_luong_tap INTEGER,
    so_cif_cccd_cmt VARCHAR(50),
    ten_khach_hang VARCHAR(500),
    phan_khach_khach_hang VARCHAR(100),
    ngay_phai_ban_giao DATE,
    ngay_ban_giao DATE,
    ngay_giai_ngan DATE,
    ngay_den_han DATE,
    loai_ho_so VARCHAR(50),
    luong_ho_so VARCHAR(100),
    phan_han_cap_td VARCHAR(50),
    ngay_du_kien_tieu_huy DATE,
    san_pham VARCHAR(100),
    trang_thai_case_pdm VARCHAR(100),
    ghi_chu TEXT,
    ma_thung VARCHAR(100),
    ngay_nhap_kho_vpbank DATE,
    ngay_chuyen_kho_crown DATE,
    khu_vuc VARCHAR(100),
    hang VARCHAR(50),
    cot VARCHAR(50),
    tinh_trang_thung VARCHAR(100),
    trang_thai_thung VARCHAR(100),
    thoi_han_cap_td INTEGER,
    ma_dao VARCHAR(100),
    ma_ts VARCHAR(100),
    rrt_id VARCHAR(100),
    ma_nq VARCHAR(100),
    
    -- Validation & Processing
    validation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, VALID, INVALID, SKIPPED
    validation_errors JSONB, -- Array of validation errors
    normalized_data JSONB, -- Normalized/cleaned data
    duplicate_key VARCHAR(500), -- For duplicate detection
    is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    master_data_exists BOOLEAN, -- Check against master data
    inserted_to_master BOOLEAN NOT NULL DEFAULT FALSE,
    inserted_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes (optimized for batch processing)
    INDEX idx_staging_job_sheet (job_id, sheet_id),
    INDEX idx_staging_validation (validation_status),
    INDEX idx_staging_duplicate (duplicate_key) WHERE duplicate_key IS NOT NULL,
    INDEX idx_staging_inserted (inserted_to_master) WHERE inserted_to_master = FALSE
) PARTITION BY RANGE (created_at); -- Partition by date for performance

-- Similar staging tables for HSBG_theo_CIF and HSBG_theo_tap
CREATE TABLE staging_hsbg_cif (...);
CREATE TABLE staging_hsbg_tap (...);
```

---

## 🔄 Migration Flow

### Phase 1: File Upload & Parsing

```java
@Service
public class ExcelMigrationService {
    
    /**
     * Step 1: Upload & Parse Excel File
     * - Validate file format
     * - Calculate file hash (duplicate detection)
     * - Parse sheets (1-3 sheets)
     * - Create migration job record
     */
    @Transactional
    public MigrationJob createMigrationJob(MultipartFile file) {
        // 1. Validate file
        validateFile(file);
        
        // 2. Calculate hash
        String fileHash = calculateFileHash(file);
        
        // 3. Check duplicate
        if (migrationJobRepository.existsByFileHash(fileHash)) {
            throw new DuplicateFileException("File already processed");
        }
        
        // 4. Parse Excel to detect sheets
        ExcelMetadata metadata = excelParserService.parseMetadata(file);
        
        // 5. Create job
        MigrationJob job = MigrationJob.builder()
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .fileHash(fileHash)
            .totalSheets(metadata.getSheetCount())
            .status(MigrationStatus.PENDING)
            .build();
        
        // 6. Create sheet records
        for (String sheetName : metadata.getSheetNames()) {
            SheetType sheetType = detectSheetType(sheetName);
            MigrationSheet sheet = MigrationSheet.builder()
                .jobId(job.getId())
                .sheetName(sheetName)
                .sheetType(sheetType)
                .totalRows(metadata.getRowCount(sheetName))
                .status(SheetStatus.PENDING)
                .build();
            migrationSheetRepository.save(sheet);
        }
        
        return job;
    }
}
```

### Phase 2: Streaming Parse & Validate (No Locking)

```java
/**
 * Step 2: Stream Parse Excel (Batch Processing)
 * - Use SXSSF streaming for large files
 * - Process in batches (10k rows/batch)
 * - Insert to staging table (no locking)
 * - Update progress in real-time
 */
@Async("excelMigrationExecutor")
public CompletableFuture<Void> processSheet(UUID sheetId) {
    MigrationSheet sheet = migrationSheetRepository.findById(sheetId)
        .orElseThrow();
    
    try {
        sheet.setStatus(SheetStatus.PROCESSING);
        sheet.setStartedAt(Instant.now());
        migrationSheetRepository.save(sheet);
        
        // Stream parse Excel
        excelFacade.readExcelWithConfig(
            inputStream,
            getDtoClass(sheet.getSheetType()),
            createStreamingConfig(),
            batch -> {
                // Process batch
                processBatch(sheetId, batch, batchNumber++);
            }
        );
        
    } catch (Exception e) {
        handleSheetError(sheetId, e);
    }
}

private void processBatch(UUID sheetId, List<DTO> batch, int batchNumber) {
    MigrationSheet sheet = migrationSheetRepository.findById(sheetId)
        .orElseThrow();
    
    // 1. Normalize & Validate batch
    List<StagingRecord> stagingRecords = batch.parallelStream()
        .map(dto -> {
            // Normalize data
            StagingRecord record = normalizeData(dto);
            
            // Validate
            ValidationResult result = validateRecord(record, sheet.getSheetType());
            record.setValidationStatus(result.isValid() ? ValidationStatus.VALID : ValidationStatus.INVALID);
            record.setValidationErrors(result.getErrors());
            
            return record;
        })
        .collect(Collectors.toList());
    
    // 2. Batch insert to staging (NO LOCKING - uses INSERT ... ON CONFLICT DO NOTHING)
    stagingRepository.batchInsert(stagingRecords);
    
    // 3. Update progress
    updateProgress(sheetId, batchNumber, batch.size(), 
                   countValid(stagingRecords), 
                   countInvalid(stagingRecords));
    
    // 4. Update heartbeat (for stuck detection)
    sheet.setLastHeartbeat(Instant.now());
    migrationSheetRepository.save(sheet);
}
```

### Phase 3: Duplicate Detection & Master Data Check

```java
/**
 * Step 3: Post-Validation (After all batches processed)
 * - Check duplicates within file
 * - Check duplicates against existing master data
 * - Check master data existence
 * - All done in staging table (no locking master tables)
 */
@Async("excelMigrationExecutor")
public CompletableFuture<Void> postValidateSheet(UUID sheetId) {
    MigrationSheet sheet = migrationSheetRepository.findById(sheetId)
        .orElseThrow();
    
    // 1. Check duplicates within file
    checkDuplicatesInFile(sheetId, sheet.getSheetType());
    
    // 2. Check duplicates against master data (using EXISTS queries - no locking)
    checkDuplicatesAgainstMaster(sheetId, sheet.getSheetType());
    
    // 3. Check master data existence (using EXISTS queries - no locking)
    checkMasterDataExistence(sheetId, sheet.getSheetType());
    
    // 4. Update sheet status
    sheet.setStatus(SheetStatus.VALIDATED);
    migrationSheetRepository.save(sheet);
}

private void checkDuplicatesInFile(UUID sheetId, SheetType sheetType) {
    // Use window functions for efficient duplicate detection
    String sql = """
        UPDATE staging_hsbg_hop_dong s
        SET is_duplicate = TRUE,
            validation_status = 'INVALID',
            validation_errors = jsonb_set(
                COALESCE(validation_errors, '[]'::jsonb),
                '{0}',
                '{"code": "DUPLICATE_IN_FILE", "message": "Duplicate record found in file"}'::jsonb
            )
        WHERE s.sheet_id = :sheetId
          AND EXISTS (
              SELECT 1
              FROM staging_hsbg_hop_dong s2
              WHERE s2.sheet_id = :sheetId
                AND s2.duplicate_key = s.duplicate_key
                AND s2.id != s.id
          )
    """;
    // Execute update
}
```

### Phase 4: Insert to Master Data (Batch, No Locking)

```java
/**
 * Step 4: Insert Valid Records to Master Data
 * - Process in batches (5k records/batch)
 * - Use INSERT ... ON CONFLICT DO UPDATE (upsert)
 * - Use batch inserts for performance
 * - Update progress in real-time
 */
@Async("excelMigrationExecutor")
public CompletableFuture<Void> insertToMaster(UUID sheetId) {
    MigrationSheet sheet = migrationSheetRepository.findById(sheetId)
        .orElseThrow();
    
    int batchSize = 5000;
    int offset = 0;
    
    while (true) {
        // 1. Fetch batch of valid records
        List<StagingRecord> batch = stagingRepository.findValidRecords(
            sheetId, offset, batchSize);
        
        if (batch.isEmpty()) {
            break;
        }
        
        // 2. Transform to master data entities
        List<MasterDataEntity> entities = batch.stream()
            .map(this::transformToMasterEntity)
            .collect(Collectors.toList());
        
        // 3. Batch upsert (no locking - uses ON CONFLICT)
        masterDataRepository.batchUpsert(entities);
        
        // 4. Mark as inserted
        stagingRepository.markAsInserted(batch.stream()
            .map(StagingRecord::getId)
            .collect(Collectors.toList()));
        
        // 5. Update progress
        updateInsertProgress(sheetId, batch.size());
        
        // 6. Update heartbeat
        sheet.setLastHeartbeat(Instant.now());
        migrationSheetRepository.save(sheet);
        
        offset += batchSize;
    }
    
    // 7. Complete sheet
    sheet.setStatus(SheetStatus.COMPLETED);
    sheet.setCompletedAt(Instant.now());
    migrationSheetRepository.save(sheet);
}
```

---

## ✅ Validation Strategy

### 1. Validation Rules per Sheet Type

```java
@Component
public class HSBGHopDongValidator implements SheetValidator {
    
    @Override
    public ValidationResult validate(StagingRecord record) {
        ValidationResult result = new ValidationResult();
        
        // CT1: Calculate "Ngày dự kiến tiêu hủy"
        LocalDate ngayDuKienTieuHuy = calculateNgayDuKienTieuHuy(
            record.getPhanHanCapTD(),
            record.getNgayDenHan()
        );
        record.setNgayDuKienTieuHuy(ngayDuKienTieuHuy);
        
        // CT2: Check duplicate by Loại hồ sơ
        if (isDuplicateByLoaiHoSo(record)) {
            result.addError("DUPLICATE", "Trùng: Số HD + Loại HS + Ngày giải ngân");
        }
        
        // CT3: Validate "Phân hạn cấp TD"
        if (!isValidPhanHanCapTD(record.getPhanHanCapTD())) {
            result.addError("INVALID_PHAN_HAN", "Phân hạn cấp TD không hợp lệ");
        }
        
        // CT4: Validate "Loại hồ sơ"
        if (!isValidLoaiHoSo(record.getLoaiHoSo())) {
            result.addError("INVALID_LOAI_HS", "Loại hồ sơ không nằm trong DS cho phép");
        }
        
        // CT5: Validate "Thời hạn cấp TD" (must be positive integer)
        if (record.getThoiHanCapTD() != null && record.getThoiHanCapTD() <= 0) {
            result.addError("INVALID_THOI_HAN", "Thời hạn cấp TD phải là số nguyên dương");
        }
        
        // CT6, CT7: Validate "Ngày đến hạn tiêu hủy"
        if (!isValidNgayDuKienTieuHuy(record)) {
            result.addError("INVALID_NGAY_TIEU_HUY", "Ngày đến hạn tiêu hủy không hợp lệ");
        }
        
        // CT8: Validate "Mã thùng" format
        if (!isValidMaThung(record.getMaThung())) {
            result.addError("INVALID_MA_THUNG", "Mã thùng không đúng format (chỉ chữ in hoa, số, dấu _)");
        }
        
        return result;
    }
    
    private LocalDate calculateNgayDuKienTieuHuy(String phanHan, LocalDate ngayDenHan) {
        if ("Vĩnh viễn".equals(phanHan)) {
            return LocalDate.of(9999, 12, 31);
        }
        if (ngayDenHan == null) {
            return LocalDate.of(9999, 12, 31);
        }
        
        return switch (phanHan) {
            case "Ngắn hạn" -> ngayDenHan.plusMonths(12 * 5);
            case "Trung hạn" -> ngayDenHan.plusMonths(12 * 10);
            case "Dài hạn" -> ngayDenHan.plusMonths(12 * 15);
            default -> LocalDate.of(9999, 12, 31);
        };
    }
    
    private boolean isValidMaThung(String maThung) {
        if (maThung == null || maThung.isEmpty()) {
            return true; // Optional field
        }
        // Only uppercase letters, numbers, and underscore
        return maThung.matches("^[A-Z0-9_]+$");
    }
}
```

### 2. Data Normalization

```java
@Component
public class DataNormalizer {
    
    public StagingRecord normalize(ExcelDTO dto, SheetType sheetType) {
        StagingRecord record = new StagingRecord();
        
        // Normalize text fields (trim, uppercase where needed)
        record.setMaThung(normalizeMaThung(dto.getMaThung()));
        record.setSoHopDong(normalizeSoHopDong(dto.getSoHopDong()));
        record.setSoCif(normalizeSoCif(dto.getSoCif()));
        
        // Normalize dates
        record.setNgayBanGiao(normalizeDate(dto.getNgayBanGiao()));
        record.setNgayGiaiNgan(normalizeDate(dto.getNgayGiaiNgan()));
        
        // Calculate derived fields
        if (sheetType == SheetType.HSBG_THEO_HOP_DONG) {
            record.setThoiHanCapTD(calculateThoiHanCapTD(
                record.getNgayDenHan(),
                record.getNgayGiaiNgan()
            ));
        }
        
        // Generate duplicate key
        record.setDuplicateKey(generateDuplicateKey(record, sheetType));
        
        return record;
    }
    
    private Integer calculateThoiHanCapTD(LocalDate ngayDenHan, LocalDate ngayGiaiNgan) {
        if (ngayDenHan == null) {
            return null;
        }
        if (ngayGiaiNgan == null) {
            return null;
        }
        
        long months = ChronoUnit.MONTHS.between(ngayGiaiNgan, ngayDenHan);
        return Math.max(1, (int) months);
    }
}
```

---

## 📈 Progress Monitoring (Real-time)

### 1. Progress Calculation

```java
@Service
public class MigrationProgressService {
    
    /**
     * Calculate real-time progress for a sheet
     */
    public ProgressInfo getSheetProgress(UUID sheetId) {
        MigrationSheet sheet = migrationSheetRepository.findById(sheetId)
            .orElseThrow();
        
        // Calculate progress percent
        double progressPercent = 0.0;
        if (sheet.getTotalRows() > 0) {
            progressPercent = (double) sheet.getProcessedRows() / sheet.getTotalRows() * 100.0;
        }
        
        // Calculate ETA
        Duration elapsed = Duration.between(sheet.getStartedAt(), Instant.now());
        long remainingRows = sheet.getTotalRows() - sheet.getProcessedRows();
        Duration estimatedRemaining = calculateETA(
            elapsed,
            sheet.getProcessedRows(),
            remainingRows
        );
        
        return ProgressInfo.builder()
            .sheetId(sheetId)
            .sheetName(sheet.getSheetName())
            .totalRows(sheet.getTotalRows())
            .processedRows(sheet.getProcessedRows())
            .validRows(sheet.getValidRows())
            .invalidRows(sheet.getInvalidRows())
            .skippedRows(sheet.getSkippedRows())
            .progressPercent(BigDecimal.valueOf(progressPercent).setScale(2, RoundingMode.HALF_UP))
            .status(sheet.getStatus())
            .elapsedTime(elapsed)
            .estimatedRemaining(estimatedRemaining)
            .lastHeartbeat(sheet.getLastHeartbeat())
            .build();
    }
    
    /**
     * Get overall job progress
     */
    public JobProgressInfo getJobProgress(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
            .orElseThrow();
        
        List<MigrationSheet> sheets = migrationSheetRepository.findByJobId(jobId);
        
        long totalRows = sheets.stream().mapToLong(MigrationSheet::getTotalRows).sum();
        long processedRows = sheets.stream().mapToLong(MigrationSheet::getProcessedRows).sum();
        
        double overallProgress = totalRows > 0 
            ? (double) processedRows / totalRows * 100.0 
            : 0.0;
        
        return JobProgressInfo.builder()
            .jobId(jobId)
            .fileName(job.getFileName())
            .totalSheets(sheets.size())
            .totalRows(totalRows)
            .processedRows(processedRows)
            .overallProgress(BigDecimal.valueOf(overallProgress).setScale(2, RoundingMode.HALF_UP))
            .sheets(sheets.stream()
                .map(this::getSheetProgress)
                .collect(Collectors.toList()))
            .status(job.getStatus())
            .build();
    }
}
```

### 2. Real-time Updates via WebSocket/SSE

```java
@RestController
@RequestMapping("/api/migration")
public class MigrationProgressController {
    
    /**
     * SSE endpoint for real-time progress updates
     */
    @GetMapping(value = "/{jobId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<JobProgressInfo>> streamProgress(@PathVariable UUID jobId) {
        return Flux.interval(Duration.ofSeconds(1))
            .map(seq -> {
                JobProgressInfo progress = progressService.getJobProgress(jobId);
                return ServerSentEvent.<JobProgressInfo>builder()
                    .id(String.valueOf(seq))
                    .event("progress")
                    .data(progress)
                    .build();
            })
            .takeWhile(progress -> 
                !progress.getData().getStatus().isTerminal()
            );
    }
}
```

---

## 🔍 Stuck Detection & Recovery

### 1. Stuck Detection Service

```java
@Service
@Scheduled(fixedDelay = 30000) // Check every 30 seconds
public class StuckDetectionService {
    
    /**
     * Detect stuck sheets (no heartbeat for > 5 minutes)
     */
    @Scheduled(fixedDelay = 30000)
    public void detectStuckSheets() {
        Instant threshold = Instant.now().minus(5, ChronoUnit.MINUTES);
        
        List<MigrationSheet> stuckSheets = migrationSheetRepository
            .findStuckSheets(threshold);
        
        for (MigrationSheet sheet : stuckSheets) {
            log.warn("Detected stuck sheet: {} (last heartbeat: {})", 
                     sheet.getId(), sheet.getLastHeartbeat());
            
            // 1. Mark as stuck
            sheet.setStatus(SheetStatus.STUCK);
            migrationSheetRepository.save(sheet);
            
            // 2. Create recovery task
            recoveryService.scheduleRecovery(sheet.getId());
        }
    }
    
    /**
     * Detect deadlocked jobs
     */
    @Scheduled(fixedDelay = 60000)
    public void detectDeadlocks() {
        // Check for long-running transactions
        List<Long> longRunningTxns = databaseMonitorService
            .findLongRunningTransactions(Duration.ofMinutes(10));
        
        if (!longRunningTxns.isEmpty()) {
            log.error("Detected {} long-running transactions", longRunningTxns.size());
            alertService.sendAlert("DEADLOCK_DETECTED", longRunningTxns);
        }
    }
}
```

### 2. Recovery Service

```java
@Service
public class RecoveryService {
    
    /**
     * Recover from stuck state
     */
    @Transactional
    public void recoverSheet(UUID sheetId) {
        MigrationSheet sheet = migrationSheetRepository.findById(sheetId)
            .orElseThrow();
        
        log.info("Recovering sheet: {}", sheetId);
        
        // 1. Check last processed row
        long lastProcessedRow = sheet.getLastProcessedRow();
        
        // 2. Resume from last processed row
        sheet.setStatus(SheetStatus.PROCESSING);
        sheet.setLastHeartbeat(Instant.now());
        migrationSheetRepository.save(sheet);
        
        // 3. Resume processing
        migrationService.resumeSheetProcessing(sheetId, lastProcessedRow);
    }
}
```

---

## ⚡ Performance Optimization

### 1. Batch Processing Configuration

```java
@Configuration
public class MigrationConfig {
    
    @Bean("excelMigrationExecutor")
    public Executor excelMigrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // Process 4 sheets concurrently
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("excel-migration-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean
    public ExcelConfig migrationExcelConfig() {
        return ExcelConfig.builder()
            .batchSize(10000) // 10k rows per batch
            .parallelProcessing(true)
            .threadPoolSize(Runtime.getRuntime().availableProcessors())
            .enableMemoryMonitoring(true)
            .memoryThresholdMB(1000) // 1GB threshold
            .maxErrorsBeforeAbort(1000) // Fail fast
            .build();
    }
}
```

### 2. Database Optimization

```sql
-- Use batch inserts with ON CONFLICT (no locking)
INSERT INTO staging_hsbg_hop_dong (...)
VALUES (...), (...), (...)
ON CONFLICT DO NOTHING;

-- Use batch upserts for master data
INSERT INTO master_data_table (...)
VALUES (...), (...), (...)
ON CONFLICT (unique_key) 
DO UPDATE SET 
    field1 = EXCLUDED.field1,
    updated_at = CURRENT_TIMESTAMP;

-- Indexes for fast duplicate detection
CREATE INDEX CONCURRENTLY idx_staging_duplicate_key 
ON staging_hsbg_hop_dong(duplicate_key) 
WHERE duplicate_key IS NOT NULL;

-- Partition staging tables by date
CREATE TABLE staging_hsbg_hop_dong_2025_11 
PARTITION OF staging_hsbg_hop_dong
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
```

### 3. Memory Management

```java
@Component
public class MemoryAwareBatchProcessor {
    
    /**
     * Adjust batch size based on available memory
     */
    public int calculateOptimalBatchSize() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long availableMemory = maxMemory - usedMemory;
        
        // Reserve 20% for other operations
        long usableMemory = (long) (availableMemory * 0.8);
        
        // Estimate memory per record (~2KB)
        int recordsPerMB = 500;
        int optimalBatchSize = (int) (usableMemory / 1024 / 1024 * recordsPerMB);
        
        // Clamp between 1000 and 50000
        return Math.max(1000, Math.min(50000, optimalBatchSize));
    }
}
```

---

## 📝 Implementation Checklist

### Phase 1: Foundation (Week 1)
- [ ] Create database schema (migration tracking, staging tables)
- [ ] Implement Excel parser service (multi-sheet support)
- [ ] Implement data normalizer
- [ ] Create DTOs for 3 sheet types
- [ ] Set up async executor configuration

### Phase 2: Validation (Week 2)
- [ ] Implement validation rules for HSBG_theo_hop_dong
- [ ] Implement validation rules for HSBG_theo_CIF
- [ ] Implement validation rules for HSBG_theo_tap
- [ ] Implement duplicate detection logic
- [ ] Implement master data existence check

### Phase 3: Processing (Week 3)
- [ ] Implement batch processing service
- [ ] Implement staging table batch insert
- [ ] Implement master data batch upsert
- [ ] Implement progress tracking
- [ ] Implement heartbeat mechanism

### Phase 4: Monitoring (Week 4)
- [ ] Implement real-time progress API (SSE/WebSocket)
- [ ] Implement stuck detection service
- [ ] Implement recovery service
- [ ] Create monitoring dashboard
- [ ] Set up alerting

### Phase 5: Testing & Optimization (Week 5)
- [ ] Load testing with 200k records
- [ ] Performance optimization
- [ ] Memory leak detection
- [ ] Deadlock detection
- [ ] End-to-end testing

---

## 🎯 Expected Performance

- **Parsing**: ~50k rows/second
- **Validation**: ~20k rows/second
- **Insert to Master**: ~10k rows/second
- **Total Time for 200k records**: ~30-40 seconds per sheet
- **Memory Usage**: < 2GB per job
- **Database Locking**: Zero (using staging tables + batch upserts)

---

## 🔗 Related Files

- `ExcelMigrationService.java` - Main service
- `MigrationProgressService.java` - Progress tracking
- `StuckDetectionService.java` - Stuck detection
- `RecoveryService.java` - Recovery logic
- `HSBGHopDongValidator.java` - Validation rules
- `MigrationProgressController.java` - REST API

