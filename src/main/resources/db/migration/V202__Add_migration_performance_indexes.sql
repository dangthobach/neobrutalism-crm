-- ============================================================================
-- Migration Performance Indexes
-- Critical indexes for 5M+ record Excel migration performance
-- ============================================================================

-- ============================================================================
-- 1. STAGING TABLES - Query Performance Indexes
-- ============================================================================

-- Index for finding valid records to insert (most critical query)
-- Covers: sheetId, validationStatus, insertedToMaster, rowNumber
CREATE INDEX idx_staging_hopdong_insert
    ON staging_hsbg_hop_dong(sheet_id, validation_status, inserted_to_master, row_number)
    WHERE validation_status = 'VALID' AND inserted_to_master = false;

CREATE INDEX idx_staging_cif_insert
    ON staging_hsbg_cif(sheet_id, validation_status, inserted_to_master, row_number)
    WHERE validation_status = 'VALID' AND inserted_to_master = false;

CREATE INDEX idx_staging_tap_insert
    ON staging_hsbg_tap(sheet_id, validation_status, inserted_to_master, row_number)
    WHERE validation_status = 'VALID' AND inserted_to_master = false;

-- Index for validation status queries
CREATE INDEX idx_staging_hopdong_validation
    ON staging_hsbg_hop_dong(sheet_id, validation_status);

CREATE INDEX idx_staging_cif_validation
    ON staging_hsbg_cif(sheet_id, validation_status);

CREATE INDEX idx_staging_tap_validation
    ON staging_hsbg_tap(sheet_id, validation_status);

-- Index for duplicate detection (using duplicate_key)
CREATE INDEX idx_staging_hopdong_duplicate
    ON staging_hsbg_hop_dong(job_id, duplicate_key)
    WHERE duplicate_key IS NOT NULL;

CREATE INDEX idx_staging_cif_duplicate
    ON staging_hsbg_cif(job_id, duplicate_key)
    WHERE duplicate_key IS NOT NULL;

CREATE INDEX idx_staging_tap_duplicate
    ON staging_hsbg_tap(job_id, duplicate_key)
    WHERE duplicate_key IS NOT NULL;

-- ============================================================================
-- 2. MIGRATION SHEETS - Progress Tracking Indexes
-- ============================================================================

-- Index for active sheet queries (by job and status)
CREATE INDEX idx_migration_sheet_job_status
    ON excel_migration_sheets(job_id, status);

-- Index for stuck detection (last_heartbeat)
CREATE INDEX idx_migration_sheet_heartbeat
    ON excel_migration_sheets(status, last_heartbeat)
    WHERE status = 'PROCESSING';

-- Index for sheet type queries
CREATE INDEX idx_migration_sheet_type
    ON excel_migration_sheets(job_id, sheet_type);

-- ============================================================================
-- 3. MIGRATION JOBS - Job Management Indexes
-- ============================================================================

-- Index for file hash duplicate detection
CREATE INDEX idx_migration_job_hash
    ON excel_migration_jobs(file_hash);

-- Index for job status queries
CREATE INDEX idx_migration_job_status
    ON excel_migration_jobs(status, created_at DESC);

-- Index for cleanup queries (old completed jobs)
CREATE INDEX idx_migration_job_cleanup
    ON excel_migration_jobs(status, completed_at)
    WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED');

-- ============================================================================
-- 4. MIGRATION ERRORS - Error Tracking Indexes
-- ============================================================================

-- Index for sheet error queries (paginated)
CREATE INDEX idx_migration_error_sheet
    ON excel_migration_errors(sheet_id, error_type, row_number);

-- Index for job-level error aggregation
CREATE INDEX idx_migration_error_job
    ON excel_migration_errors(job_id, sheet_id, error_type);

-- Index for error type analysis
CREATE INDEX idx_migration_error_type
    ON excel_migration_errors(error_type, sheet_id);

-- ============================================================================
-- 5. PERFORMANCE COMMENTS
-- ============================================================================

-- Staging tables:
COMMENT ON INDEX idx_staging_hopdong_insert IS 'Critical for findValidRecordsForInsert query - prevents full table scan on 200k+ records';
COMMENT ON INDEX idx_staging_hopdong_duplicate IS 'Enables fast duplicate detection within job scope';

-- Migration sheets:
COMMENT ON INDEX idx_migration_sheet_job_status IS 'Fast lookup for job progress tracking';
COMMENT ON INDEX idx_migration_sheet_heartbeat IS 'Enables stuck detection service to quickly find stalled sheets';

-- Migration errors:
COMMENT ON INDEX idx_migration_error_sheet IS 'Enables efficient paginated error retrieval for UI';

-- ============================================================================
-- 6. OPTIONAL: PARTITIONING HINTS (PostgreSQL 10+)
-- ============================================================================

-- For very large staging tables (5M+ records), consider partitioning by job_id:
--
-- ALTER TABLE staging_hsbg_hop_dong PARTITION BY HASH (job_id);
-- CREATE TABLE staging_hsbg_hop_dong_p0 PARTITION OF staging_hsbg_hop_dong
--     FOR VALUES WITH (MODULUS 4, REMAINDER 0);
-- CREATE TABLE staging_hsbg_hop_dong_p1 PARTITION OF staging_hsbg_hop_dong
--     FOR VALUES WITH (MODULUS 4, REMAINDER 1);
-- ... repeat for p2, p3
--
-- This distributes 5M records across 4 partitions (~1.25M each) for better performance

-- ============================================================================
-- 7. STATISTICS UPDATE
-- ============================================================================

-- Ensure statistics are up to date for query planner
ANALYZE staging_hsbg_hop_dong;
ANALYZE staging_hsbg_cif;
ANALYZE staging_hsbg_tap;
ANALYZE excel_migration_sheets;
ANALYZE excel_migration_jobs;
ANALYZE excel_migration_errors;
