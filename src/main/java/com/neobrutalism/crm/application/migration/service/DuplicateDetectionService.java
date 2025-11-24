package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.model.SheetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for detecting duplicates within file and against master data
 * All errors are logged to migration_errors table (single source of truth)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Check duplicates within file based on sheet type
     */
    @Transactional
    public void checkDuplicatesInFile(UUID sheetId, SheetType sheetType) {
        switch (sheetType) {
            case HSBG_THEO_HOP_DONG -> checkDuplicatesInFileHopDong(sheetId);
            case HSBG_THEO_CIF -> checkDuplicatesInFileCif(sheetId);
            case HSBG_THEO_TAP -> checkDuplicatesInFileTap(sheetId);
        }
    }
    
    /**
     * Check duplicates within file for HSBG_theo_hop_dong
     * Marks duplicates and logs errors to migration_errors table
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void checkDuplicatesInFileHopDong(UUID sheetId) {
        log.info("Checking duplicates in file for sheet: {}", sheetId);

        // Step 1: Mark duplicates in staging table
        String updateSql = """
            UPDATE staging_hsbg_hop_dong s1
            SET is_duplicate = TRUE,
                validation_status = 'INVALID'
            WHERE s1.sheet_id = ?
              AND s1.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1
                  FROM staging_hsbg_hop_dong s2
                  WHERE s2.sheet_id = ?
                    AND s2.duplicate_key = s1.duplicate_key
                    AND s2.id != s1.id
                    AND s2.validation_status = 'VALID'
              )
        """;

        int updated = jdbcTemplate.update(updateSql, sheetId, sheetId);
        log.info("Marked {} duplicate records in file for sheet: {}", updated, sheetId);

        // Step 2: Log duplicate errors to migration_errors table
        if (updated > 0) {
            String insertErrorsSql = """
                INSERT INTO migration_errors (
                    sheet_id,
                    row_number,
                    batch_number,
                    error_code,
                    error_message,
                    validation_rule,
                    error_data,
                    created_at
                )
                SELECT
                    s1.sheet_id,
                    s1.row_number,
                    0 as batch_number,
                    'DUPLICATE_IN_FILE' as error_code,
                    'Trùng lặp trong file: Số hợp đồng + Loại hồ sơ + Ngày giải ngân' as error_message,
                    'UNIQUE_KEY_CT2' as validation_rule,
                    jsonb_build_object(
                        'duplicate_key', s1.duplicate_key,
                        'fields', jsonb_build_object(
                            'so_hop_dong', s1.so_hop_dong,
                            'loai_ho_so', s1.loai_ho_so,
                            'ngay_giai_ngan', s1.ngay_giai_ngan
                        ),
                        'conflicting_rows', (
                            SELECT jsonb_agg(s2.row_number ORDER BY s2.row_number)
                            FROM staging_hsbg_hop_dong s2
                            WHERE s2.sheet_id = s1.sheet_id
                              AND s2.duplicate_key = s1.duplicate_key
                              AND s2.id != s1.id
                        )
                    ) as error_data,
                    NOW() as created_at
                FROM staging_hsbg_hop_dong s1
                WHERE s1.sheet_id = ?
                  AND s1.is_duplicate = TRUE
            """;

            int inserted = jdbcTemplate.update(insertErrorsSql, sheetId);
            log.info("Logged {} duplicate errors to migration_errors table for sheet: {}", inserted, sheetId);
        }
    }
    
    /**
     * Check duplicates within file for HSBG_theo_CIF
     * Marks duplicates and logs errors to migration_errors table
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void checkDuplicatesInFileCif(UUID sheetId) {
        log.info("Checking duplicates in file for CIF sheet: {}", sheetId);

        // Step 1: Mark duplicates
        String updateSql = """
            UPDATE staging_hsbg_cif s1
            SET is_duplicate = TRUE,
                validation_status = 'INVALID'
            WHERE s1.sheet_id = ?
              AND s1.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1
                  FROM staging_hsbg_cif s2
                  WHERE s2.sheet_id = ?
                    AND s2.duplicate_key = s1.duplicate_key
                    AND s2.id != s1.id
                    AND s2.validation_status = 'VALID'
              )
        """;

        int updated = jdbcTemplate.update(updateSql, sheetId, sheetId);
        log.info("Marked {} duplicate records in file for CIF sheet: {}", updated, sheetId);

        // Step 2: Log errors
        if (updated > 0) {
            String insertErrorsSql = """
                INSERT INTO migration_errors (
                    sheet_id,
                    row_number,
                    batch_number,
                    error_code,
                    error_message,
                    validation_rule,
                    error_data,
                    created_at
                )
                SELECT
                    s1.sheet_id,
                    s1.row_number,
                    0 as batch_number,
                    'DUPLICATE_IN_FILE' as error_code,
                    'Trùng lặp trong file: CIF + Ngày giải ngân + Loại hồ sơ' as error_message,
                    'UNIQUE_KEY_CIF' as validation_rule,
                    jsonb_build_object(
                        'duplicate_key', s1.duplicate_key,
                        'fields', jsonb_build_object(
                            'so_cif', s1.so_cif,
                            'ngay_giai_ngan', s1.ngay_giai_ngan,
                            'loai_ho_so', s1.loai_ho_so
                        ),
                        'conflicting_rows', (
                            SELECT jsonb_agg(s2.row_number ORDER BY s2.row_number)
                            FROM staging_hsbg_cif s2
                            WHERE s2.sheet_id = s1.sheet_id
                              AND s2.duplicate_key = s1.duplicate_key
                              AND s2.id != s1.id
                        )
                    ) as error_data,
                    NOW() as created_at
                FROM staging_hsbg_cif s1
                WHERE s1.sheet_id = ?
                  AND s1.is_duplicate = TRUE
            """;

            int inserted = jdbcTemplate.update(insertErrorsSql, sheetId);
            log.info("Logged {} duplicate errors to migration_errors table for CIF sheet: {}", inserted, sheetId);
        }
    }
    
    /**
     * Check duplicates within file for HSBG_theo_tap
     * Marks duplicates and logs errors to migration_errors table
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void checkDuplicatesInFileTap(UUID sheetId) {
        log.info("Checking duplicates in file for Tap sheet: {}", sheetId);

        // Step 1: Mark duplicates
        String updateSql = """
            UPDATE staging_hsbg_tap s1
            SET is_duplicate = TRUE,
                validation_status = 'INVALID'
            WHERE s1.sheet_id = ?
              AND s1.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1
                  FROM staging_hsbg_tap s2
                  WHERE s2.sheet_id = ?
                    AND s2.duplicate_key = s1.duplicate_key
                    AND s2.id != s1.id
                    AND s2.validation_status = 'VALID'
              )
        """;

        int updated = jdbcTemplate.update(updateSql, sheetId, sheetId);
        log.info("Marked {} duplicate records in file for Tap sheet: {}", updated, sheetId);

        // Step 2: Log errors
        if (updated > 0) {
            String insertErrorsSql = """
                INSERT INTO migration_errors (
                    sheet_id,
                    row_number,
                    batch_number,
                    error_code,
                    error_message,
                    validation_rule,
                    error_data,
                    created_at
                )
                SELECT
                    s1.sheet_id,
                    s1.row_number,
                    0 as batch_number,
                    'DUPLICATE_IN_FILE' as error_code,
                    'Trùng lặp trong file: Mã ĐV + TNBG + Tháng PS + Sản phẩm' as error_message,
                    'UNIQUE_KEY_TAP' as validation_rule,
                    jsonb_build_object(
                        'duplicate_key', s1.duplicate_key,
                        'fields', jsonb_build_object(
                            'ma_don_vi', s1.ma_don_vi,
                            'trach_nhiem_ban_giao', s1.trach_nhiem_ban_giao,
                            'thang_phat_sinh', s1.thang_phat_sinh,
                            'san_pham', s1.san_pham
                        ),
                        'conflicting_rows', (
                            SELECT jsonb_agg(s2.row_number ORDER BY s2.row_number)
                            FROM staging_hsbg_tap s2
                            WHERE s2.sheet_id = s1.sheet_id
                              AND s2.duplicate_key = s1.duplicate_key
                              AND s2.id != s1.id
                        )
                    ) as error_data,
                    NOW() as created_at
                FROM staging_hsbg_tap s1
                WHERE s1.sheet_id = ?
                  AND s1.is_duplicate = TRUE
            """;

            int inserted = jdbcTemplate.update(insertErrorsSql, sheetId);
            log.info("Logged {} duplicate errors to migration_errors table for Tap sheet: {}", inserted, sheetId);
        }
    }
    
    /**
     * Check duplicates against master data (no locking - uses EXISTS)
     */
    @Transactional(readOnly = true)
    public void checkDuplicatesAgainstMaster(UUID sheetId, SheetType sheetType) {
        log.info("Checking duplicates against master data for sheet: {}", sheetId);
        
        // This will check against master data tables using EXISTS queries
        // No locking because we're only reading
        switch (sheetType) {
            case HSBG_THEO_HOP_DONG -> checkDuplicatesAgainstMasterHopDong(sheetId);
            case HSBG_THEO_CIF -> checkDuplicatesAgainstMasterCif(sheetId);
            case HSBG_THEO_TAP -> checkDuplicatesAgainstMasterTap(sheetId);
        }
    }
    
    private void checkDuplicatesAgainstMasterHopDong(UUID sheetId) {
        log.info("Checking duplicates against master data (contracts) for sheet: {}", sheetId);
        
        String updateSql = """
            UPDATE staging_hsbg_hop_dong s
            SET master_data_exists = TRUE,
                validation_status = 'DUPLICATE'
            WHERE s.sheet_id = ?
              AND s.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1 FROM contracts m
                  WHERE m.contract_number = s.so_hop_dong
                    AND m.is_deleted = FALSE
                    AND m.tenant_id = (SELECT tenant_id FROM migration_jobs WHERE id = s.job_id)
              )
            """;
        
        int updated = jdbcTemplate.update(updateSql, sheetId);
        log.info("Marked {} HopDong records as duplicates against master data", updated);
        
        // Log errors for duplicates
        if (updated > 0) {
            String logErrorSql = """
                INSERT INTO migration_errors (id, job_id, sheet_id, staging_record_id, row_number, error_type, error_message, created_at)
                SELECT 
                    gen_random_uuid(),
                    s.job_id,
                    s.sheet_id,
                    s.id,
                    s.row_number,
                    'DUPLICATE',
                    CONCAT('Contract ', s.so_hop_dong, ' already exists in master data'),
                    CURRENT_TIMESTAMP
                FROM staging_hsbg_hop_dong s
                WHERE s.sheet_id = ?
                  AND s.master_data_exists = TRUE
                  AND s.validation_status = 'DUPLICATE'
                """;
            
            jdbcTemplate.update(logErrorSql, sheetId);
        }
    }
    
    private void checkDuplicatesAgainstMasterCif(UUID sheetId) {
        log.info("Checking duplicates against master data (customers) for sheet: {}", sheetId);
        
        String updateSql = """
            UPDATE staging_hsbg_cif s
            SET master_data_exists = TRUE,
                validation_status = 'DUPLICATE'
            WHERE s.sheet_id = ?
              AND s.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1 FROM customers m
                  WHERE m.code = s.so_cif
                    AND m.deleted = FALSE
                    AND m.tenant_id = (SELECT CAST(tenant_id AS TEXT) FROM migration_jobs WHERE id = s.job_id)
              )
            """;
        
        int updated = jdbcTemplate.update(updateSql, sheetId);
        log.info("Marked {} CIF records as duplicates against master data", updated);
        
        // Log errors for duplicates
        if (updated > 0) {
            String logErrorSql = """
                INSERT INTO migration_errors (id, job_id, sheet_id, staging_record_id, row_number, error_type, error_message, created_at)
                SELECT 
                    gen_random_uuid(),
                    s.job_id,
                    s.sheet_id,
                    s.id,
                    s.row_number,
                    'DUPLICATE',
                    CONCAT('Customer CIF ', s.so_cif, ' already exists in master data'),
                    CURRENT_TIMESTAMP
                FROM staging_hsbg_cif s
                WHERE s.sheet_id = ?
                  AND s.master_data_exists = TRUE
                  AND s.validation_status = 'DUPLICATE'
                """;
            
            jdbcTemplate.update(logErrorSql, sheetId);
        }
    }
    
    private void checkDuplicatesAgainstMasterTap(UUID sheetId) {
        log.info("Checking duplicates against master data (document volumes) for sheet: {}", sheetId);
        
        String updateSql = """
            UPDATE staging_hsbg_tap s
            SET master_data_exists = TRUE,
                validation_status = 'DUPLICATE'
            WHERE s.sheet_id = ?
              AND s.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1 FROM document_volumes m
                  WHERE m.volume_name = s.ten_tap
                    AND m.box_code = s.ma_thung
                    AND m.is_deleted = FALSE
                    AND m.tenant_id = (SELECT tenant_id FROM migration_jobs WHERE id = s.job_id)
              )
            """;
        
        int updated = jdbcTemplate.update(updateSql, sheetId);
        log.info("Marked {} Tap records as duplicates against master data", updated);
        
        // Log errors for duplicates
        if (updated > 0) {
            String logErrorSql = """
                INSERT INTO migration_errors (id, job_id, sheet_id, staging_record_id, row_number, error_type, error_message, created_at)
                SELECT 
                    gen_random_uuid(),
                    s.job_id,
                    s.sheet_id,
                    s.id,
                    s.row_number,
                    'DUPLICATE',
                    CONCAT('Document volume ', s.ten_tap, ' in box ', s.ma_thung, ' already exists in master data'),
                    CURRENT_TIMESTAMP
                FROM staging_hsbg_tap s
                WHERE s.sheet_id = ?
                  AND s.master_data_exists = TRUE
                  AND s.validation_status = 'DUPLICATE'
                """;
            
            jdbcTemplate.update(logErrorSql, sheetId);
        }
    }
}

