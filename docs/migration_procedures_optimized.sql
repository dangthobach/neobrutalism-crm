-- ============================================================
-- OPTIMIZED MIGRATION PROCEDURES FOR HSBG
-- Version: 3.0 - Procedure with Performance Optimization
-- ============================================================
-- Features:
--   1. Procedures instead of functions for better transaction control
--   2. Row-level locking optimization (SELECT FOR UPDATE SKIP LOCKED)
--   3. Batch commit strategy to avoid long locks
--   4. Parallel-safe duplicate detection
--   5. Proper indexing hints
-- ============================================================

-- ============================================================
-- 1. HSBG_HOP_DONG MIGRATION PROCEDURE
-- ============================================================

CREATE OR REPLACE PROCEDURE migrate_hsbg_hop_dong(
    IN p_job_id UUID,
    IN p_batch_size INTEGER DEFAULT 1000,
    OUT o_total_processed INTEGER,
    OUT o_migrated_count INTEGER,
    OUT o_duplicate_count INTEGER,
    OUT o_error_count INTEGER,
    OUT o_warning_count INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_batch_number INTEGER := 0;
    v_batch_count INTEGER := 0;
    v_rec_count INTEGER;
    v_delivery_method VARCHAR(50) := 'PHYSICAL';
BEGIN
    v_start_time := clock_timestamp();
    o_total_processed := 0;
    o_migrated_count := 0;
    o_duplicate_count := 0;
    o_error_count := 0;
    o_warning_count := 0;

    RAISE NOTICE '[%] ======================================== MIGRATION START ========================================', v_start_time;
    RAISE NOTICE '[%] Migration Type: HSBG_HOP_DONG | Job ID: % | Batch Size: %', v_start_time, p_job_id, p_batch_size;

    -- ============================================================
    -- STEP 0: VALIDATION
    -- ============================================================
    RAISE NOTICE '[%] Step 0: Validating input data...', clock_timestamp();

    IF NOT EXISTS (
        SELECT 1 FROM staging_hsbg_hop_dong
        WHERE job_id = p_job_id
          AND inserted_to_master = false
          AND validation_status = 'VALID'
          AND is_duplicate = false
        LIMIT 1
    ) THEN
        RAISE EXCEPTION 'No valid records found for job_id=%', p_job_id;
    END IF;

    -- ============================================================
    -- STEP 1: CREATE TEMPORARY WORKING TABLES
    -- ============================================================
    RAISE NOTICE '[%] Step 1: Creating temporary tables...', clock_timestamp();

    DROP TABLE IF EXISTS _tmp_migration_batch;
    DROP TABLE IF EXISTS _tmp_department_map;
    DROP TABLE IF EXISTS _tmp_responsibility_map;
    DROP TABLE IF EXISTS _tmp_warehouse_map;
    DROP TABLE IF EXISTS _tmp_box_groups;
    DROP TABLE IF EXISTS _tmp_duplicate_check;
    DROP TABLE IF EXISTS _tmp_warnings;

    CREATE TEMP TABLE _tmp_warnings (
        warning_type TEXT,
        message TEXT,
        detail JSONB
    ) ON COMMIT DROP;

    -- ============================================================
    -- STEP 2: FOREIGN KEY MAPPING (Read-only, no locks)
    -- ============================================================
    RAISE NOTICE '[%] Step 2: Creating foreign key mappings...', clock_timestamp();

    CREATE TEMP TABLE _tmp_department_map AS
    SELECT DISTINCT
        s.ma_don_vi,
        d.id as department_id,
        CASE WHEN d.id IS NULL THEN true ELSE false END as is_invalid
    FROM staging_hsbg_hop_dong s
    LEFT JOIN pdms_departments d ON d.code = s.ma_don_vi
    WHERE s.job_id = p_job_id
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND s.ma_don_vi IS NOT NULL;

    CREATE TEMP TABLE _tmp_responsibility_map AS
    SELECT DISTINCT
        s.trach_nhiem_ban_giao,
        r.id as responsibility_id,
        CASE WHEN r.id IS NULL THEN true ELSE false END as is_invalid
    FROM staging_hsbg_hop_dong s
    LEFT JOIN pdms_responsibilities r ON r.name = s.trach_nhiem_ban_giao
    WHERE s.job_id = p_job_id
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND s.trach_nhiem_ban_giao IS NOT NULL;

    CREATE TEMP TABLE _tmp_warehouse_map AS
    SELECT DISTINCT
        s.kho_vpbank,
        w.id as warehouse_id,
        CASE WHEN w.id IS NULL THEN true ELSE false END as is_invalid
    FROM staging_hsbg_hop_dong s
    LEFT JOIN pdms_vpbank_warehouse w ON w.warehouse_name = s.kho_vpbank
    WHERE s.job_id = p_job_id
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND s.kho_vpbank IS NOT NULL;

    RAISE NOTICE '[%] Step 2: Mappings created - Depts: %, Resp: %, Warehouse: %',
        clock_timestamp(),
        (SELECT COUNT(*) FROM _tmp_department_map WHERE is_invalid = false),
        (SELECT COUNT(*) FROM _tmp_responsibility_map WHERE is_invalid = false),
        (SELECT COUNT(*) FROM _tmp_warehouse_map WHERE is_invalid = false);

    -- ============================================================
    -- STEP 3: DUPLICATE DETECTION (Read-only with indexes)
    -- ============================================================
    RAISE NOTICE '[%] Step 3: Detecting duplicates against master data...', clock_timestamp();

    CREATE TEMP TABLE _tmp_duplicate_check AS
    WITH valid_records AS (
        SELECT
            s.id as staging_id,
            s.loai_ho_so,
            s.so_hop_dong,
            s.so_cif_cccd_cmt,
            s.ngay_giai_ngan,
            dm.department_id,
            CASE s.loai_ho_so
                WHEN 'LD' THEN 'contract_disbursement'
                WHEN 'MD' THEN 'contract_disbursement'
                WHEN 'OD' THEN 'contract_disbursement'
                WHEN 'HDHM' THEN 'contract_disbursement'
                WHEN 'KSSV' THEN 'contract_disbursement'
                WHEN 'BAO_THANH_TOAN' THEN 'contract_disbursement'
                WHEN 'BIEN_NHAN_THE_CHAP' THEN 'contract_disbursement'
                WHEN 'CC' THEN 'contract_cif'
                WHEN 'TSBD' THEN 'contract_cif'
                WHEN 'TTK' THEN 'contract_cif_department_disbursement'
                ELSE 'unknown'
            END as check_type
        FROM staging_hsbg_hop_dong s
        LEFT JOIN _tmp_department_map dm ON dm.ma_don_vi = s.ma_don_vi
        WHERE s.job_id = p_job_id
          AND s.inserted_to_master = false
          AND s.validation_status = 'VALID'
          AND s.is_duplicate = false
          AND (dm.is_invalid = false OR s.ma_don_vi IS NULL)
    )
    SELECT
        vr.staging_id,
        vr.check_type,
        vr.loai_ho_so,
        vr.so_hop_dong,
        CASE
            WHEN vr.check_type = 'contract_disbursement' THEN
                (SELECT p.id FROM pdms_credit_casepdm p
                 WHERE p.contract_number = vr.so_hop_dong
                   AND p.record_type = vr.loai_ho_so
                   AND p.disbursement_date = vr.ngay_giai_ngan
                   AND p.delivery_method = v_delivery_method
                 LIMIT 1)
            WHEN vr.check_type = 'contract_cif' THEN
                (SELECT p.id FROM pdms_credit_casepdm p
                 WHERE p.contract_number = vr.so_hop_dong
                   AND p.record_type = vr.loai_ho_so
                   AND p.cif_number = vr.so_cif_cccd_cmt
                   AND p.delivery_method = v_delivery_method
                 LIMIT 1)
            WHEN vr.check_type = 'contract_cif_department_disbursement' THEN
                (SELECT p.id FROM pdms_credit_casepdm p
                 WHERE p.contract_number = vr.so_hop_dong
                   AND p.record_type = vr.loai_ho_so
                   AND p.cif_number = vr.so_cif_cccd_cmt
                   AND p.department_id = vr.department_id
                   AND p.disbursement_date = vr.ngay_giai_ngan
                   AND p.delivery_method = v_delivery_method
                 LIMIT 1)
            ELSE NULL
        END as existing_pdm_id
    FROM valid_records vr;

    CREATE INDEX idx_tmp_dup_staging ON _tmp_duplicate_check(staging_id);
    CREATE INDEX idx_tmp_dup_existing ON _tmp_duplicate_check(existing_pdm_id) WHERE existing_pdm_id IS NOT NULL;

    SELECT COUNT(*) INTO o_duplicate_count FROM _tmp_duplicate_check WHERE existing_pdm_id IS NOT NULL;

    RAISE NOTICE '[%] Step 3: Found % duplicates in master data', clock_timestamp(), o_duplicate_count;

    -- ============================================================
    -- STEP 4: GROUP BY BOX (Prepare box inserts)
    -- ============================================================
    RAISE NOTICE '[%] Step 4: Grouping records by box...', clock_timestamp();

    CREATE TEMP TABLE _tmp_box_groups AS
    SELECT
        s.ma_thung,
        dm.department_id,
        wm.warehouse_id,
        rm.responsibility_id,
        s.khu_vuc as area,
        s.hang as row_position,
        s.cot as column_position,
        s.tinh_trang_thung as state,
        s.trang_thai_thung as status,
        s.ngay_nhap_kho_vpbank as warehouse_import_date,
        s.ngay_chuyen_kho_crown as crown_transfer_date,
        s.loai_ho_so as document_type,
        s.phan_han_cap_td as storage_period,
        MIN(s.id) as first_record_id,
        COUNT(*) as record_count
    FROM staging_hsbg_hop_dong s
    LEFT JOIN _tmp_department_map dm ON dm.ma_don_vi = s.ma_don_vi
    LEFT JOIN _tmp_responsibility_map rm ON rm.trach_nhiem_ban_giao = s.trach_nhiem_ban_giao
    LEFT JOIN _tmp_warehouse_map wm ON wm.kho_vpbank = s.kho_vpbank
    LEFT JOIN _tmp_duplicate_check dc ON dc.staging_id = s.id
    WHERE s.job_id = p_job_id
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND dc.existing_pdm_id IS NULL
      AND s.ma_thung IS NOT NULL
      AND (dm.is_invalid = false OR s.ma_don_vi IS NULL)
      AND (rm.is_invalid = false OR s.trach_nhiem_ban_giao IS NULL)
      AND (wm.is_invalid = false OR s.kho_vpbank IS NULL)
    GROUP BY
        s.ma_thung, dm.department_id, wm.warehouse_id, rm.responsibility_id,
        s.khu_vuc, s.hang, s.cot, s.tinh_trang_thung, s.trang_thai_thung,
        s.ngay_nhap_kho_vpbank, s.ngay_chuyen_kho_crown, s.loai_ho_so, s.phan_han_cap_td;

    RAISE NOTICE '[%] Step 4: Identified % unique boxes', clock_timestamp(), (SELECT COUNT(*) FROM _tmp_box_groups);

    -- ============================================================
    -- STEP 5: BATCH MIGRATION WITH ROW-LEVEL LOCKING
    -- ============================================================
    RAISE NOTICE '[%] Step 5: Starting batch migration with optimized locking...', clock_timestamp();

    -- Calculate total batches
    SELECT COUNT(*) INTO o_total_processed
    FROM staging_hsbg_hop_dong s
    LEFT JOIN _tmp_department_map dm ON dm.ma_don_vi = s.ma_don_vi
    LEFT JOIN _tmp_responsibility_map rm ON rm.trach_nhiem_ban_giao = s.trach_nhiem_ban_giao
    LEFT JOIN _tmp_warehouse_map wm ON wm.kho_vpbank = s.kho_vpbank
    LEFT JOIN _tmp_duplicate_check dc ON dc.staging_id = s.id
    WHERE s.job_id = p_job_id
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND dc.existing_pdm_id IS NULL
      AND (dm.is_invalid = false OR s.ma_don_vi IS NULL)
      AND (rm.is_invalid = false OR s.trach_nhiem_ban_giao IS NULL)
      AND (wm.is_invalid = false OR s.kho_vpbank IS NULL);

    v_batch_count := CEIL(o_total_processed::DECIMAL / p_batch_size);

    RAISE NOTICE '[%] Step 5: Total valid records: % | Batches: %', clock_timestamp(), o_total_processed, v_batch_count;

    -- Process each batch
    FOR v_batch_number IN 1..v_batch_count LOOP
        RAISE NOTICE '[%]   ========== Batch %/% ==========', clock_timestamp(), v_batch_number, v_batch_count;

        -- **OPTIMIZATION: Use SELECT FOR UPDATE SKIP LOCKED to avoid blocking**
        DROP TABLE IF EXISTS _tmp_migration_batch;
        CREATE TEMP TABLE _tmp_migration_batch AS
        SELECT
            s.id,
            s.ma_thung,
            s.ten_tap,
            s.so_hop_dong,
            s.so_cif_cccd_cmt,
            s.ten_khach_hang,
            s.ngay_giai_ngan,
            s.ngay_ban_giao,
            s.ngay_den_han,
            s.loai_ho_so,
            s.phan_han_cap_td,
            s.phan_khach_khach_hang,
            s.san_pham,
            s.ma_nq,
            s.thoi_han_cap_td,
            s.ma_dao,
            s.ma_ts,
            s.rrt_id,
            s.ghi_chu,
            s.ngay_du_kien_tieu_huy,
            s.trang_thai_case_pdm,
            s.created_at,
            dm.department_id,
            rm.responsibility_id,
            wm.warehouse_id
        FROM staging_hsbg_hop_dong s
        LEFT JOIN _tmp_department_map dm ON dm.ma_don_vi = s.ma_don_vi
        LEFT JOIN _tmp_responsibility_map rm ON rm.trach_nhiem_ban_giao = s.trach_nhiem_ban_giao
        LEFT JOIN _tmp_warehouse_map wm ON wm.kho_vpbank = s.kho_vpbank
        LEFT JOIN _tmp_duplicate_check dc ON dc.staging_id = s.id
        WHERE s.job_id = p_job_id
          AND s.inserted_to_master = false
          AND s.validation_status = 'VALID'
          AND s.is_duplicate = false
          AND dc.existing_pdm_id IS NULL
          AND (dm.is_invalid = false OR s.ma_don_vi IS NULL)
          AND (rm.is_invalid = false OR s.trach_nhiem_ban_giao IS NULL)
          AND (wm.is_invalid = false OR s.kho_vpbank IS NULL)
        ORDER BY s.created_at, s.id
        LIMIT p_batch_size
        FOR UPDATE OF s SKIP LOCKED;  -- **KEY OPTIMIZATION: Skip locked rows**

        GET DIAGNOSTICS v_rec_count = ROW_COUNT;

        IF v_rec_count = 0 THEN
            RAISE NOTICE '[%]   Batch %: No more records to process (all locked by other processes)', clock_timestamp(), v_batch_number;
            EXIT;
        END IF;

        RAISE NOTICE '[%]   Batch %: Processing % records', clock_timestamp(), v_batch_number, v_rec_count;

        -- ============================================================
        -- 5.1) INSERT BOXES (with conflict handling)
        -- ============================================================
        WITH box_inserts AS (
            INSERT INTO pdms_box (
                id, box_code, department_id, warehouse_id, responsibility_id,
                area, row_position, column_position, state, status,
                warehouse_import_date, crown_transfer_date, document_type, storage_period,
                is_migration, is_delete, created_date, last_modified_date, created_by, last_modified_by
            )
            SELECT DISTINCT
                gen_random_uuid(),
                bg.ma_thung,
                bg.department_id,
                bg.warehouse_id,
                bg.responsibility_id,
                bg.area,
                CAST(bg.row_position AS INTEGER),
                CAST(bg.column_position AS INTEGER),
                COALESCE(bg.state, 'GOOD')::VARCHAR,
                COALESCE(bg.status, 'ACTIVE')::VARCHAR,
                bg.warehouse_import_date,
                bg.crown_transfer_date,
                bg.document_type::VARCHAR,
                bg.storage_period::VARCHAR,
                true, false, NOW(), NOW(), 'MIGRATION_SYSTEM', 'MIGRATION_SYSTEM'
            FROM _tmp_box_groups bg
            WHERE bg.ma_thung IN (SELECT DISTINCT mb.ma_thung FROM _tmp_migration_batch mb WHERE mb.ma_thung IS NOT NULL)
              AND NOT EXISTS (SELECT 1 FROM pdms_box b WHERE b.box_code = bg.ma_thung)
            ON CONFLICT (box_code) DO NOTHING
            RETURNING id, box_code
        )
        SELECT COUNT(*) INTO v_rec_count FROM box_inserts;
        RAISE NOTICE '[%]   Batch %: Inserted % new boxes', clock_timestamp(), v_batch_number, v_rec_count;

        -- ============================================================
        -- 5.2) INSERT CREDIT CASE PDM
        -- ============================================================
        WITH pdm_inserts AS (
            INSERT INTO pdms_credit_casepdm (
                id, pdm_code, document_type, flow_type, status, delivery_method,
                contract_number, cif_number, customer_name, disbursement_date,
                credit_extension, record_type, department_id, responsibility_id,
                handover_date, customer_segmentation, product, nq_code,
                due_date, credit_term, dao_code, ts_code, rrt_id, note,
                expected_destruction_date, box_id, confirm,
                created_date, last_modified_date, created_by, last_modified_by
            )
            SELECT
                gen_random_uuid(),
                FORMAT('PDMC-%08d', nextval('pdms_credit_casepdm_code_seq')),
                COALESCE(mb.loai_ho_so, 'LD')::VARCHAR,
                'STANDARD'::VARCHAR,
                COALESCE(mb.trang_thai_case_pdm, 'ACTIVE')::VARCHAR,
                v_delivery_method::VARCHAR,
                mb.so_hop_dong,
                mb.so_cif_cccd_cmt,
                mb.ten_khach_hang,
                mb.ngay_giai_ngan,
                mb.phan_han_cap_td::VARCHAR,
                mb.loai_ho_so::VARCHAR,
                mb.department_id,
                mb.responsibility_id,
                mb.ngay_ban_giao,
                mb.phan_khach_khach_hang,
                mb.san_pham::VARCHAR,
                mb.ma_nq,
                mb.ngay_den_han,
                mb.thoi_han_cap_td,
                mb.ma_dao,
                mb.ma_ts,
                mb.rrt_id,
                mb.ghi_chu,
                mb.ngay_du_kien_tieu_huy,
                (SELECT b.id FROM pdms_box b WHERE b.box_code = mb.ma_thung LIMIT 1),
                false,
                COALESCE(mb.created_at, NOW()), NOW(), 'MIGRATION_SYSTEM', 'MIGRATION_SYSTEM'
            FROM _tmp_migration_batch mb
            RETURNING id, pdm_code
        )
        SELECT COUNT(*) INTO v_rec_count FROM pdm_inserts;
        RAISE NOTICE '[%]   Batch %: Inserted % PDM cases', clock_timestamp(), v_batch_number, v_rec_count;

        -- ============================================================
        -- 5.3) INSERT CREDIT DOCUMENTS
        -- ============================================================
        WITH credit_doc_inserts AS (
            INSERT INTO pdms_credit_document (
                id, single_document, document_name, note, box_id,
                created_date, last_modified_date, created_by, last_modified_by
            )
            SELECT DISTINCT
                gen_random_uuid(),
                false,
                COALESCE(mb.ten_tap, 'UNNAMED_FOLDER'),
                mb.ghi_chu,
                (SELECT b.id FROM pdms_box b WHERE b.box_code = mb.ma_thung LIMIT 1),
                NOW(), NOW(), 'MIGRATION_SYSTEM', 'MIGRATION_SYSTEM'
            FROM _tmp_migration_batch mb
            WHERE mb.ten_tap IS NOT NULL
            GROUP BY mb.ten_tap, mb.ghi_chu, mb.ma_thung
            ON CONFLICT DO NOTHING
            RETURNING id
        )
        SELECT COUNT(*) INTO v_rec_count FROM credit_doc_inserts;
        RAISE NOTICE '[%]   Batch %: Inserted % credit documents', clock_timestamp(), v_batch_number, v_rec_count;

        -- ============================================================
        -- 5.4) LINK DOCUMENTS TO CASES
        -- ============================================================
        WITH link_inserts AS (
            INSERT INTO credit_document_pdm_case (credit_document_id, credit_pdm_case_id)
            SELECT DISTINCT
                cd.id,
                pdm.id
            FROM _tmp_migration_batch mb
            INNER JOIN pdms_credit_document cd
                ON cd.document_name = COALESCE(mb.ten_tap, 'UNNAMED_FOLDER')
                AND cd.box_id = (SELECT b.id FROM pdms_box b WHERE b.box_code = mb.ma_thung LIMIT 1)
            INNER JOIN pdms_credit_casepdm pdm
                ON pdm.contract_number = mb.so_hop_dong
                AND COALESCE(pdm.cif_number, '') = COALESCE(mb.so_cif_cccd_cmt, '')
                AND COALESCE(pdm.disbursement_date, '1900-01-01'::date) = COALESCE(mb.ngay_giai_ngan, '1900-01-01'::date)
                AND pdm.created_by = 'MIGRATION_SYSTEM'
            WHERE NOT EXISTS (
                SELECT 1 FROM credit_document_pdm_case cdpc
                WHERE cdpc.credit_document_id = cd.id AND cdpc.credit_pdm_case_id = pdm.id
            )
            ON CONFLICT DO NOTHING
            RETURNING credit_document_id
        )
        SELECT COUNT(*) INTO v_rec_count FROM link_inserts;
        RAISE NOTICE '[%]   Batch %: Linked % relationships', clock_timestamp(), v_batch_number, v_rec_count;

        -- ============================================================
        -- 5.5) UPDATE STAGING TABLE
        -- ============================================================
        UPDATE staging_hsbg_hop_dong s
        SET inserted_to_master = true,
            master_data_exists = true,
            inserted_at = NOW()
        WHERE s.id IN (SELECT id FROM _tmp_migration_batch);

        GET DIAGNOSTICS v_rec_count = ROW_COUNT;
        o_migrated_count := o_migrated_count + v_rec_count;

        RAISE NOTICE '[%]   Batch %: Updated % staging records | Total migrated: %', clock_timestamp(), v_batch_number, v_rec_count, o_migrated_count;

        -- **COMMIT AFTER EACH BATCH to release locks**
        COMMIT;

    END LOOP;

    -- ============================================================
    -- STEP 6: UPDATE DUPLICATES IN STAGING
    -- ============================================================
    RAISE NOTICE '[%] Step 6: Marking duplicates in staging...', clock_timestamp();

    UPDATE staging_hsbg_hop_dong s
    SET is_duplicate = true,
        duplicate_key = 'PDM_ID:' || dc.existing_pdm_id::text,
        master_data_exists = true
    FROM _tmp_duplicate_check dc
    WHERE s.id = dc.staging_id
      AND dc.existing_pdm_id IS NOT NULL;

    -- ============================================================
    -- STEP 7: COLLECT WARNINGS & ERRORS
    -- ============================================================
    RAISE NOTICE '[%] Step 7: Collecting warnings...', clock_timestamp();

    -- Count validation errors
    SELECT COUNT(*) INTO o_error_count
    FROM staging_hsbg_hop_dong
    WHERE job_id = p_job_id
      AND inserted_to_master = false
      AND validation_status = 'INVALID';

    -- Create warnings table if not exists
    CREATE TABLE IF NOT EXISTS migration_warnings (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        job_id UUID NOT NULL,
        migration_type TEXT NOT NULL,
        warning_type TEXT NOT NULL,
        message TEXT,
        detail JSONB,
        created_at TIMESTAMP DEFAULT NOW()
    );

    -- Log warnings
    IF o_duplicate_count > 0 THEN
        INSERT INTO _tmp_warnings (warning_type, message, detail)
        VALUES (
            'DUPLICATE_RECORDS',
            FORMAT('Found %s duplicates in master data', o_duplicate_count),
            jsonb_build_object('job_id', p_job_id, 'count', o_duplicate_count)
        );
    END IF;

    IF o_error_count > 0 THEN
        INSERT INTO _tmp_warnings (warning_type, message, detail)
        VALUES (
            'VALIDATION_ERRORS',
            FORMAT('%s records skipped due to validation errors', o_error_count),
            jsonb_build_object('job_id', p_job_id, 'count', o_error_count)
        );
    END IF;

    -- Save to permanent table
    INSERT INTO migration_warnings (job_id, migration_type, warning_type, message, detail)
    SELECT p_job_id, 'HSBG_HOP_DONG', warning_type, message, detail FROM _tmp_warnings;

    SELECT COUNT(*) INTO o_warning_count FROM _tmp_warnings;

    -- ============================================================
    -- FINAL SUMMARY
    -- ============================================================
    RAISE NOTICE '[%] ======================================== MIGRATION SUMMARY ========================================', clock_timestamp();
    RAISE NOTICE '[%] Type: HSBG_HOP_DONG | Job ID: %', clock_timestamp(), p_job_id;
    RAISE NOTICE '[%] Total Valid: % | Migrated: % | Duplicates: % | Errors: % | Warnings: %',
        clock_timestamp(), o_total_processed, o_migrated_count, o_duplicate_count, o_error_count, o_warning_count;
    RAISE NOTICE '[%] Duration: % | Batches: % | Batch Size: %',
        clock_timestamp(), clock_timestamp() - v_start_time, v_batch_count, p_batch_size;
    RAISE NOTICE '[%] ====================================================================================================', clock_timestamp();

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '[%] ❌ MIGRATION FAILED: % | SQLSTATE: %', clock_timestamp(), SQLERRM, SQLSTATE;
END;
$$;

-- Grant permissions
COMMENT ON PROCEDURE migrate_hsbg_hop_dong(UUID, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER) IS
'Optimized migration procedure for HSBG_HOP_DONG with row-level locking and batch commits';

-- ============================================================
-- 2. HSBG_CIF MIGRATION PROCEDURE
-- ============================================================

CREATE OR REPLACE PROCEDURE migrate_hsbg_cif(
    IN p_job_id UUID,
    IN p_batch_size INTEGER DEFAULT 1000,
    OUT o_total_processed INTEGER,
    OUT o_migrated_count INTEGER,
    OUT o_duplicate_count INTEGER,
    OUT o_error_count INTEGER,
    OUT o_warning_count INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_batch_number INTEGER := 0;
    v_batch_count INTEGER := 0;
    v_rec_count INTEGER;
    v_delivery_method VARCHAR(50) := 'PHYSICAL';
BEGIN
    v_start_time := clock_timestamp();
    o_total_processed := 0;
    o_migrated_count := 0;
    o_duplicate_count := 0;
    o_error_count := 0;
    o_warning_count := 0;

    RAISE NOTICE '[%] ======================================== MIGRATION START ========================================', v_start_time;
    RAISE NOTICE '[%] Migration Type: HSBG_CIF | Job ID: % | Batch Size: %', v_start_time, p_job_id, p_batch_size;

    -- Similar implementation as HSBG_HOP_DONG but for staging_hsbg_cif table
    -- Duplicate check logic: CIF + Ngày giải ngân + Loại hồ sơ
    -- Document grouping by: ten_tap (folder name)

    -- TODO: Implementation follows same pattern as migrate_hsbg_hop_dong
    -- Key differences:
    --   - Query from staging_hsbg_cif instead of staging_hsbg_hop_dong
    --   - Duplicate key: so_cif + ngay_giai_ngan + loai_ho_so
    --   - No ma_dao, ma_ts fields (CIF-specific schema)

    RAISE NOTICE '[%] ⚠️  HSBG_CIF migration not yet implemented', clock_timestamp();

END;
$$;

-- ============================================================
-- 3. HSBG_TAP MIGRATION PROCEDURE
-- ============================================================

CREATE OR REPLACE PROCEDURE migrate_hsbg_tap(
    IN p_job_id UUID,
    IN p_batch_size INTEGER DEFAULT 1000,
    OUT o_total_processed INTEGER,
    OUT o_migrated_count INTEGER,
    OUT o_duplicate_count INTEGER,
    OUT o_error_count INTEGER,
    OUT o_warning_count INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_batch_number INTEGER := 0;
    v_batch_count INTEGER := 0;
    v_rec_count INTEGER;
    v_delivery_method VARCHAR(50) := 'PHYSICAL';
BEGIN
    v_start_time := clock_timestamp();
    o_total_processed := 0;
    o_migrated_count := 0;
    o_duplicate_count := 0;
    o_error_count := 0;
    o_warning_count := 0;

    RAISE NOTICE '[%] ======================================== MIGRATION START ========================================', v_start_time;
    RAISE NOTICE '[%] Migration Type: HSBG_TAP | Job ID: % | Batch Size: %', v_start_time, p_job_id, p_batch_size;

    -- Similar implementation as HSBG_HOP_DONG but for staging_hsbg_tap table
    -- Duplicate check logic: Mã ĐV + TNBG + Tháng PS + Sản phẩm
    -- Document grouping by: ten_tap (folder name)

    -- TODO: Implementation follows same pattern as migrate_hsbg_hop_dong
    -- Key differences:
    --   - Query from staging_hsbg_tap instead of staging_hsbg_hop_dong
    --   - Duplicate key: ma_don_vi + trach_nhiem_ban_giao + thang_phat_sinh + san_pham
    --   - Use occurrence_month (thang_phat_sinh) instead of disbursement_date

    RAISE NOTICE '[%] ⚠️  HSBG_TAP migration not yet implemented', clock_timestamp();

END;
$$;

-- ============================================================
-- HELPER FUNCTION: Get Migration Status
-- ============================================================

CREATE OR REPLACE FUNCTION get_migration_status(p_job_id UUID)
RETURNS TABLE (
    migration_type TEXT,
    total_records BIGINT,
    migrated BIGINT,
    pending BIGINT,
    duplicates BIGINT,
    errors BIGINT,
    completion_pct NUMERIC(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        'HSBG_HOP_DONG'::TEXT,
        COUNT(*)::BIGINT,
        COUNT(*) FILTER (WHERE inserted_to_master = true)::BIGINT,
        COUNT(*) FILTER (WHERE inserted_to_master = false AND validation_status = 'VALID')::BIGINT,
        COUNT(*) FILTER (WHERE is_duplicate = true)::BIGINT,
        COUNT(*) FILTER (WHERE validation_status = 'INVALID')::BIGINT,
        ROUND((COUNT(*) FILTER (WHERE inserted_to_master = true)::NUMERIC / NULLIF(COUNT(*), 0)) * 100, 2)
    FROM staging_hsbg_hop_dong
    WHERE job_id = p_job_id

    UNION ALL

    SELECT
        'HSBG_CIF'::TEXT,
        COUNT(*)::BIGINT,
        COUNT(*) FILTER (WHERE inserted_to_master = true)::BIGINT,
        COUNT(*) FILTER (WHERE inserted_to_master = false AND validation_status = 'VALID')::BIGINT,
        COUNT(*) FILTER (WHERE is_duplicate = true)::BIGINT,
        COUNT(*) FILTER (WHERE validation_status = 'INVALID')::BIGINT,
        ROUND((COUNT(*) FILTER (WHERE inserted_to_master = true)::NUMERIC / NULLIF(COUNT(*), 0)) * 100, 2)
    FROM staging_hsbg_cif
    WHERE job_id = p_job_id

    UNION ALL

    SELECT
        'HSBG_TAP'::TEXT,
        COUNT(*)::BIGINT,
        COUNT(*) FILTER (WHERE inserted_to_master = true)::BIGINT,
        COUNT(*) FILTER (WHERE inserted_to_master = false AND validation_status = 'VALID')::BIGINT,
        COUNT(*) FILTER (WHERE is_duplicate = true)::BIGINT,
        COUNT(*) FILTER (WHERE validation_status = 'INVALID')::BIGINT,
        ROUND((COUNT(*) FILTER (WHERE inserted_to_master = true)::NUMERIC / NULLIF(COUNT(*), 0)) * 100, 2)
    FROM staging_hsbg_tap
    WHERE job_id = p_job_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_migration_status(UUID) IS 'Get real-time migration status for all sheet types';

-- ============================================================
-- USAGE EXAMPLES
-- ============================================================

-- Example 1: Run migration for HSBG_HOP_DONG
/*
DO $$
DECLARE
    v_total INTEGER;
    v_migrated INTEGER;
    v_duplicates INTEGER;
    v_errors INTEGER;
    v_warnings INTEGER;
BEGIN
    CALL migrate_hsbg_hop_dong(
        'your-job-id-here'::UUID,
        1000,  -- batch size
        v_total,
        v_migrated,
        v_duplicates,
        v_errors,
        v_warnings
    );

    RAISE NOTICE 'Migration completed: % migrated out of % valid records', v_migrated, v_total;
END $$;
*/

-- Example 2: Check migration status
/*
SELECT * FROM get_migration_status('your-job-id-here'::UUID);
*/

-- Example 3: View migration warnings
/*
SELECT *
FROM migration_warnings
WHERE job_id = 'your-job-id-here'::UUID
ORDER BY created_at DESC;
*/
