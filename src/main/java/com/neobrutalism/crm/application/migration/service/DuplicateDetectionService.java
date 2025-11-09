package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.model.SheetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for detecting duplicates within file and against master data
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
     */
    @Transactional
    private void checkDuplicatesInFileHopDong(UUID sheetId) {
        log.info("Checking duplicates in file for sheet: {}", sheetId);
        
        // Mark duplicates based on business rule CT2
        String sql = """
            UPDATE staging_hsbg_hop_dong s1
            SET is_duplicate = TRUE,
                validation_status = 'INVALID',
                validation_errors = jsonb_set(
                    COALESCE(validation_errors, '[]'::jsonb),
                    '{0}',
                    '{"code": "DUPLICATE_IN_FILE", "message": "Trùng: Số HD + Loại HS + Ngày giải ngân"}'::jsonb
                )
            WHERE s1.sheet_id = :sheetId
              AND s1.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1
                  FROM staging_hsbg_hop_dong s2
                  WHERE s2.sheet_id = :sheetId
                    AND s2.duplicate_key = s1.duplicate_key
                    AND s2.id != s1.id
                    AND s2.validation_status = 'VALID'
              )
        """;
        
        int updated = jdbcTemplate.update(sql, sheetId, sheetId);
        log.info("Marked {} duplicate records in file for sheet: {}", updated, sheetId);
    }
    
    /**
     * Check duplicates within file for HSBG_theo_CIF
     */
    @Transactional
    private void checkDuplicatesInFileCif(UUID sheetId) {
        log.info("Checking duplicates in file for CIF sheet: {}", sheetId);
        
        // Key: Số CIF + Ngày giải ngân + Loại HS
        String sql = """
            UPDATE staging_hsbg_cif s1
            SET is_duplicate = TRUE,
                validation_status = 'INVALID',
                validation_errors = jsonb_set(
                    COALESCE(validation_errors, '[]'::jsonb),
                    '{0}',
                    '{"code": "DUPLICATE_IN_FILE", "message": "Trùng: CIF + Ngày giải ngân + Loại HS"}'::jsonb
                )
            WHERE s1.sheet_id = :sheetId
              AND s1.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1
                  FROM staging_hsbg_cif s2
                  WHERE s2.sheet_id = :sheetId
                    AND s2.duplicate_key = s1.duplicate_key
                    AND s2.id != s1.id
                    AND s2.validation_status = 'VALID'
              )
        """;
        
        int updated = jdbcTemplate.update(sql, sheetId, sheetId);
        log.info("Marked {} duplicate records in file for CIF sheet: {}", updated, sheetId);
    }
    
    /**
     * Check duplicates within file for HSBG_theo_tap
     */
    @Transactional
    private void checkDuplicatesInFileTap(UUID sheetId) {
        log.info("Checking duplicates in file for Tap sheet: {}", sheetId);
        
        // Key: Mã DV + TNBG + Tháng phát sinh + Sản phẩm
        String sql = """
            UPDATE staging_hsbg_tap s1
            SET is_duplicate = TRUE,
                validation_status = 'INVALID',
                validation_errors = jsonb_set(
                    COALESCE(validation_errors, '[]'::jsonb),
                    '{0}',
                    '{"code": "DUPLICATE_IN_FILE", "message": "Trùng: Mã DV + TNBG + Tháng PS + Sản phẩm"}'::jsonb
                )
            WHERE s1.sheet_id = :sheetId
              AND s1.validation_status = 'VALID'
              AND EXISTS (
                  SELECT 1
                  FROM staging_hsbg_tap s2
                  WHERE s2.sheet_id = :sheetId
                    AND s2.duplicate_key = s1.duplicate_key
                    AND s2.id != s1.id
                    AND s2.validation_status = 'VALID'
              )
        """;
        
        int updated = jdbcTemplate.update(sql, sheetId, sheetId);
        log.info("Marked {} duplicate records in file for Tap sheet: {}", updated, sheetId);
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
        // TODO: Implement based on master data table structure
        // Example:
        // UPDATE staging_hsbg_hop_dong s
        // SET master_data_exists = TRUE
        // WHERE EXISTS (
        //     SELECT 1 FROM master_data_table m
        //     WHERE m.contract_number = s.so_hop_dong
        //       AND m.document_type = s.loai_ho_so
        //       AND m.disbursement_date = s.ngay_giai_ngan
        // )
    }
    
    private void checkDuplicatesAgainstMasterCif(UUID sheetId) {
        // TODO: Implement
    }
    
    private void checkDuplicatesAgainstMasterTap(UUID sheetId) {
        // TODO: Implement
    }
}

