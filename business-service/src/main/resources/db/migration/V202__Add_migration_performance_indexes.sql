-- ============================================================================
-- Migration Performance Indexes
-- Critical indexes for 5M+ record Excel migration performance
-- ============================================================================

-- ============================================================================
-- 1. STAGING TABLES - Query Performance Indexes
-- ============================================================================

-- Index for finding valid records to insert (most critical query)
-- Covers: sheetId, validationStatus, insertedToMaster, rowNumber
CREATE INDEX IF NOT EXISTS idx_staging_hopdong_insert
    ON staging_hsbg_hop_dong(sheet_id, validation_status, inserted_to_master, row_number);

CREATE INDEX IF NOT EXISTS idx_staging_cif_insert
    ON staging_hsbg_cif(sheet_id, validation_status, inserted_to_master, row_number);

CREATE INDEX IF NOT EXISTS idx_staging_tap_insert
    ON staging_hsbg_tap(sheet_id, validation_status, inserted_to_master, row_number);

-- Index for validation status queries
CREATE INDEX IF NOT EXISTS idx_staging_hopdong_validation
    ON staging_hsbg_hop_dong(sheet_id, validation_status);

CREATE INDEX IF NOT EXISTS idx_staging_cif_validation
    ON staging_hsbg_cif(sheet_id, validation_status);

CREATE INDEX IF NOT EXISTS idx_staging_tap_validation
    ON staging_hsbg_tap(sheet_id, validation_status);

-- Index for duplicate detection (using duplicate_key)
CREATE INDEX IF NOT EXISTS idx_staging_hopdong_duplicate
    ON staging_hsbg_hop_dong(job_id, duplicate_key);

CREATE INDEX IF NOT EXISTS idx_staging_cif_duplicate
    ON staging_hsbg_cif(job_id, duplicate_key);

CREATE INDEX IF NOT EXISTS idx_staging_tap_duplicate
    ON staging_hsbg_tap(job_id, duplicate_key);

-- ============================================================================
-- 2. MIGRATION SHEETS - Progress Tracking Indexes
-- ============================================================================

-- Index for active sheet queries (by job and status)
CREATE INDEX IF NOT EXISTS idx_migration_sheet_job_status
    ON excel_migration_sheets(job_id, status);

-- Index for stuck detection (last_heartbeat)
CREATE INDEX IF NOT EXISTS idx_migration_sheet_heartbeat
    ON excel_migration_sheets(status, last_heartbeat);

-- Index for sheet type queries
CREATE INDEX IF NOT EXISTS idx_migration_sheet_type
    ON excel_migration_sheets(job_id, sheet_type);

-- ============================================================================
-- 3. MIGRATION JOBS - Job Management Indexes
-- ============================================================================

-- Index for file hash duplicate detection
CREATE INDEX IF NOT EXISTS idx_migration_job_hash
    ON excel_migration_jobs(file_hash);

-- Index for job status queries
CREATE INDEX IF NOT EXISTS idx_migration_job_status
    ON excel_migration_jobs(status, created_at DESC);

-- Index for cleanup queries (old completed jobs)
CREATE INDEX IF NOT EXISTS idx_migration_job_cleanup
    ON excel_migration_jobs(status, completed_at);

-- ============================================================================
-- 4. MIGRATION ERRORS - Error Tracking Indexes
-- ============================================================================

-- Index for sheet error queries (paginated)
CREATE INDEX IF NOT EXISTS idx_migration_error_sheet
    ON excel_migration_errors(sheet_id, error_type, row_number);

-- Index for job-level error aggregation
CREATE INDEX IF NOT EXISTS idx_migration_error_job
    ON excel_migration_errors(job_id, sheet_id, error_type);

-- Index for error type analysis
CREATE INDEX IF NOT EXISTS idx_migration_error_type
    ON excel_migration_errors(error_type, sheet_id);

-- ============================================================================
-- 5. PERFORMANCE COMMENTS
-- ============================================================================

-- Staging tables:
-- Comments: H2 doesn't support COMMENT ON INDEX statements
-- See code documentation for index descriptions

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

-- ANALYZE statements are PostgreSQL-specific
-- H2 doesn't require explicit ANALYZE - statistics are maintained automatically
