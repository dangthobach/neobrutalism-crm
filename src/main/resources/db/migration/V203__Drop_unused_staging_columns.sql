-- =============================================
-- V203: Drop Unused Staging Table Columns
-- =============================================
-- Purpose: Remove validation_errors and normalized_data columns
--          from staging tables as all errors are now logged to
--          excel_migration_errors table (single source of truth)
-- =============================================

-- Drop validation_errors and normalized_data from staging_hsbg_hop_dong
ALTER TABLE staging_hsbg_hop_dong
    DROP COLUMN IF EXISTS validation_errors,
    DROP COLUMN IF EXISTS normalized_data;

-- Drop validation_errors and normalized_data from staging_hsbg_cif
ALTER TABLE staging_hsbg_cif
    DROP COLUMN IF EXISTS validation_errors,
    DROP COLUMN IF EXISTS normalized_data;

-- Drop validation_errors and normalized_data from staging_hsbg_tap
ALTER TABLE staging_hsbg_tap
    DROP COLUMN IF EXISTS validation_errors,
    DROP COLUMN IF EXISTS normalized_data;

-- Add comment explaining the change
COMMENT ON TABLE staging_hsbg_hop_dong IS 'Staging table for HSBG_theo_hop_dong. All errors logged to excel_migration_errors table.';
COMMENT ON TABLE staging_hsbg_cif IS 'Staging table for HSBG_theo_CIF. All errors logged to excel_migration_errors table.';
COMMENT ON TABLE staging_hsbg_tap IS 'Staging table for HSBG_theo_tap. All errors logged to excel_migration_errors table.';
