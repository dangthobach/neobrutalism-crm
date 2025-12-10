-- =============================================
-- V201: Excel Migration Tracking Tables
-- =============================================
-- Purpose: Support high-performance Excel data migration
-- with real-time progress monitoring, stuck detection, and recovery
-- =============================================

-- Migration Job Tracking
CREATE TABLE IF NOT EXISTS excel_migration_jobs (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    total_sheets INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    error_stack_trace TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_migration_status 
ON excel_migration_jobs(status);

CREATE INDEX IF NOT EXISTS idx_migration_created_at 
ON excel_migration_jobs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_migration_file_hash 
ON excel_migration_jobs(file_hash);

-- Migration Sheet Tracking
CREATE TABLE IF NOT EXISTS excel_migration_sheets (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    job_id UUID NOT NULL REFERENCES excel_migration_jobs(id) ON DELETE CASCADE,
    sheet_name VARCHAR(100) NOT NULL,
    sheet_type VARCHAR(50) NOT NULL,
    total_rows BIGINT NOT NULL,
    processed_rows BIGINT NOT NULL DEFAULT 0,
    valid_rows BIGINT NOT NULL DEFAULT 0,
    invalid_rows BIGINT NOT NULL DEFAULT 0,
    skipped_rows BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    last_processed_row BIGINT NOT NULL DEFAULT 0,
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_sheet_job_name UNIQUE (job_id, sheet_name)
);

CREATE INDEX IF NOT EXISTS idx_sheet_job_id 
ON excel_migration_sheets(job_id);

CREATE INDEX IF NOT EXISTS idx_sheet_status 
ON excel_migration_sheets(status);

CREATE INDEX IF NOT EXISTS idx_sheet_heartbeat 
ON excel_migration_sheets(last_heartbeat);

-- Migration Progress Tracking
CREATE TABLE IF NOT EXISTS excel_migration_progress (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    sheet_id UUID NOT NULL REFERENCES excel_migration_sheets(id) ON DELETE CASCADE,
    batch_number INTEGER NOT NULL,
    batch_size INTEGER NOT NULL,
    processed_count INTEGER NOT NULL,
    valid_count INTEGER NOT NULL,
    invalid_count INTEGER NOT NULL,
    skipped_count INTEGER NOT NULL,
    processing_time_ms BIGINT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_progress_sheet_id 
ON excel_migration_progress(sheet_id, batch_number);

-- Migration Errors
CREATE TABLE IF NOT EXISTS excel_migration_errors (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    sheet_id UUID NOT NULL REFERENCES excel_migration_sheets(id) ON DELETE CASCADE,
    row_number BIGINT NOT NULL,
    batch_number INTEGER,
    error_code VARCHAR(50) NOT NULL,
    error_message TEXT NOT NULL,
    error_data CLOB,
    validation_rule VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_error_sheet_id 
ON excel_migration_errors(sheet_id);

CREATE INDEX IF NOT EXISTS idx_error_code 
ON excel_migration_errors(error_code);

CREATE INDEX IF NOT EXISTS idx_error_row 
ON excel_migration_errors(sheet_id, row_number);

-- Staging Table for HSBG_theo_hop_dong
CREATE TABLE IF NOT EXISTS staging_hsbg_hop_dong (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    job_id UUID NOT NULL,
    sheet_id UUID NOT NULL,
    row_number BIGINT NOT NULL,
    
    -- Data fields
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
    
    -- Processing fields
    validation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    validation_errors CLOB,
    normalized_data CLOB,
    duplicate_key VARCHAR(500),
    is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    master_data_exists BOOLEAN,
    inserted_to_master BOOLEAN NOT NULL DEFAULT FALSE,
    inserted_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_staging_job_sheet 
ON staging_hsbg_hop_dong(job_id, sheet_id);

CREATE INDEX IF NOT EXISTS idx_staging_validation 
ON staging_hsbg_hop_dong(validation_status);

CREATE INDEX IF NOT EXISTS idx_staging_duplicate 
ON staging_hsbg_hop_dong(duplicate_key);

CREATE INDEX IF NOT EXISTS idx_staging_inserted 
ON staging_hsbg_hop_dong(inserted_to_master);

-- Staging Table for HSBG_theo_CIF
CREATE TABLE IF NOT EXISTS staging_hsbg_cif (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    job_id UUID NOT NULL,
    sheet_id UUID NOT NULL,
    row_number BIGINT NOT NULL,
    
    -- Data fields
    kho_vpbank VARCHAR(255),
    ma_don_vi VARCHAR(100),
    trach_nhiem_ban_giao VARCHAR(255),
    so_cif VARCHAR(50),
    ten_khach_hang VARCHAR(500),
    ten_tap VARCHAR(255),
    so_luong_tap INTEGER,
    phan_khach_khach_hang VARCHAR(100),
    ngay_phai_ban_giao DATE,
    ngay_ban_giao DATE,
    ngay_giai_ngan DATE,
    loai_ho_so VARCHAR(50),
    luong_ho_so VARCHAR(100),
    phan_han_cap_td VARCHAR(50),
    san_pham VARCHAR(100),
    trang_thai_case_pdm VARCHAR(100),
    ghi_chu TEXT,
    ma_nq VARCHAR(100),
    ma_thung VARCHAR(100),
    ngay_nhap_kho_vpbank DATE,
    ngay_chuyen_kho_crown DATE,
    khu_vuc VARCHAR(100),
    hang VARCHAR(50),
    cot VARCHAR(50),
    tinh_trang_thung VARCHAR(100),
    trang_thai_thung VARCHAR(100),
    
    -- Processing fields
    validation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    validation_errors CLOB,
    normalized_data CLOB,
    duplicate_key VARCHAR(500),
    is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    master_data_exists BOOLEAN,
    inserted_to_master BOOLEAN NOT NULL DEFAULT FALSE,
    inserted_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_staging_cif_job_sheet 
ON staging_hsbg_cif(job_id, sheet_id);

CREATE INDEX IF NOT EXISTS idx_staging_cif_validation 
ON staging_hsbg_cif(validation_status);

CREATE INDEX IF NOT EXISTS idx_staging_cif_duplicate 
ON staging_hsbg_cif(duplicate_key);

CREATE INDEX IF NOT EXISTS idx_staging_cif_inserted 
ON staging_hsbg_cif(inserted_to_master);

-- Staging Table for HSBG_theo_tap
CREATE TABLE IF NOT EXISTS staging_hsbg_tap (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    job_id UUID NOT NULL,
    sheet_id UUID NOT NULL,
    row_number BIGINT NOT NULL,
    
    -- Data fields
    kho_vpbank VARCHAR(255),
    ma_don_vi VARCHAR(100),
    trach_nhiem_ban_giao VARCHAR(255),
    thang_phat_sinh DATE,
    ten_tap VARCHAR(255),
    so_luong_tap INTEGER,
    ngay_phai_ban_giao DATE,
    ngay_ban_giao DATE,
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
    
    -- Processing fields
    validation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    validation_errors CLOB,
    normalized_data CLOB,
    duplicate_key VARCHAR(500),
    is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    master_data_exists BOOLEAN,
    inserted_to_master BOOLEAN NOT NULL DEFAULT FALSE,
    inserted_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_staging_tap_job_sheet 
ON staging_hsbg_tap(job_id, sheet_id);

CREATE INDEX IF NOT EXISTS idx_staging_tap_validation 
ON staging_hsbg_tap(validation_status);

CREATE INDEX IF NOT EXISTS idx_staging_tap_duplicate 
ON staging_hsbg_tap(duplicate_key);

CREATE INDEX IF NOT EXISTS idx_staging_tap_inserted 
ON staging_hsbg_tap(inserted_to_master);

-- Comments: H2 doesn't support COMMENT ON statements
-- See code documentation for table descriptions

