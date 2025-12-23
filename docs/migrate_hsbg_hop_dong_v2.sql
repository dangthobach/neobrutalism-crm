-- ============================================================
-- MIGRATION STORED PROCEDURE FOR HSBG_HOP_DONG
-- Version: 2.0 - Entity-based Migration
-- ============================================================
-- Migrates data from staging_hsbg_hop_dong to:
--   1. pdms_box (BoxEntity)
--   2. pdms_credit_casepdm (CreditCasePdmEntity)
--   3. pdms_credit_document (CreditDocumentEntity)
--   4. credit_document_pdm_case (Many-to-Many relationship)
-- ============================================================

CREATE OR REPLACE FUNCTION migrate_hsbg_hop_dong(
    IN job_id_in UUID,
    IN batch_size_in INTEGER DEFAULT 1000
)
RETURNS TABLE(
    total_processed INTEGER,
    migrated_count INTEGER,
    duplicate_count INTEGER,
    error_count INTEGER,
    warning_count INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    rec RECORD;
    existing_pdm_id UUID;
    existing_box_id UUID;
    delivery_method_val VARCHAR(50) := 'PHYSICAL';
    new_pdm_id UUID;
    new_box_id UUID;
    new_credit_doc_id UUID;
    v_total_processed INTEGER := 0;
    v_migrated_count INTEGER := 0;
    v_duplicate_count INTEGER := 0;
    v_error_count INTEGER := 0;
    v_warning_count INTEGER := 0;
    v_start_time TIMESTAMP;
    rec_warn RECORD;
    v_batch_number INTEGER := 0;
    v_batch_count INTEGER := 0;
BEGIN
    v_start_time := clock_timestamp();

    RAISE NOTICE '[%] ======================================== MIGRATION START ========================================', v_start_time;
    RAISE NOTICE '[%] Migration Type: HSBG_HOP_DONG', v_start_time;
    RAISE NOTICE '[%] Job ID: %', v_start_time, job_id_in;
    RAISE NOTICE '[%] Batch Size: %', v_start_time, batch_size_in;
    RAISE NOTICE '[%] ============================================================================================', v_start_time;

    -- ============================================================
    -- 0) VALIDATION & CLEANUP
    -- ============================================================
    RAISE NOTICE '[%] Step 0: Validation and cleanup...', clock_timestamp();

    -- Check if valid records exist
    IF NOT EXISTS (
        SELECT 1 FROM staging_hsbg_hop_dong
        WHERE job_id = job_id_in
          AND inserted_to_master = false
          AND validation_status = 'VALID'
          AND is_duplicate = false
        LIMIT 1
    ) THEN
        RAISE EXCEPTION 'No valid records found for job_id=%. Either all records have validation errors, are duplicates, or already migrated.', job_id_in;
    END IF;

    -- Cleanup temporary tables
    DROP TABLE IF EXISTS _migration_warnings;
    DROP TABLE IF EXISTS _valid_records;
    DROP TABLE IF EXISTS _duplicate_check;
    DROP TABLE IF EXISTS _department_map;
    DROP TABLE IF EXISTS _responsibility_map;
    DROP TABLE IF EXISTS _warehouse_map;
    DROP TABLE IF EXISTS _migration_batch;
    DROP TABLE IF EXISTS _box_groups;

    CREATE TEMP TABLE _migration_warnings (
        warning_type TEXT,
        message TEXT,
        detail JSONB
    );

    RAISE NOTICE '[%] Step 0 completed: Validation successful', clock_timestamp();

    -- ============================================================
    -- 1) FOREIGN KEY VALIDATION & MAPPING
    -- ============================================================
    RAISE NOTICE '[%] Step 1: Creating foreign key mappings...', clock_timestamp();

    -- **Department mapping**
    CREATE TEMP TABLE _department_map AS
    SELECT DISTINCT
        s.ma_don_vi,
        d.id as department_id,
        d.code as department_code,
        CASE WHEN d.id IS NULL THEN true ELSE false END as is_invalid
    FROM staging_hsbg_hop_dong s
    LEFT JOIN pdms_departments d ON d.code = s.ma_don_vi
    WHERE s.job_id = job_id_in
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND s.ma_don_vi IS NOT NULL;

    RAISE NOTICE '[%]   - Departments mapped: %', clock_timestamp(), (SELECT COUNT(*) FROM _department_map WHERE is_invalid = false);
    RAISE NOTICE '[%]   - Invalid departments: %', clock_timestamp(), (SELECT COUNT(*) FROM _department_map WHERE is_invalid = true);

    -- **Responsibility mapping**
    CREATE TEMP TABLE _responsibility_map AS
    SELECT DISTINCT
        s.trach_nhiem_ban_giao,
        r.id as responsibility_id,
        r.name as responsibility_name,
        CASE WHEN r.id IS NULL THEN true ELSE false END as is_invalid
    FROM staging_hsbg_hop_dong s
    LEFT JOIN pdms_responsibilities r ON r.name = s.trach_nhiem_ban_giao
    WHERE s.job_id = job_id_in
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND s.trach_nhiem_ban_giao IS NOT NULL;

    RAISE NOTICE '[%]   - Responsibilities mapped: %', clock_timestamp(), (SELECT COUNT(*) FROM _responsibility_map WHERE is_invalid = false);
    RAISE NOTICE '[%]   - Invalid responsibilities: %', clock_timestamp(), (SELECT COUNT(*) FROM _responsibility_map WHERE is_invalid = true);

    -- **Warehouse mapping**
    CREATE TEMP TABLE _warehouse_map AS
    SELECT DISTINCT
        s.kho_vpbank,
        w.id as warehouse_id,
        w.warehouse_name,
        CASE WHEN w.id IS NULL THEN true ELSE false END as is_invalid
    FROM staging_hsbg_hop_dong s
    LEFT JOIN pdms_vpbank_warehouse w ON w.warehouse_name = s.kho_vpbank
    WHERE s.job_id = job_id_in
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND s.kho_vpbank IS NOT NULL;

    RAISE NOTICE '[%]   - Warehouses mapped: %', clock_timestamp(), (SELECT COUNT(*) FROM _warehouse_map WHERE is_invalid = false);
    RAISE NOTICE '[%]   - Invalid warehouses: %', clock_timestamp(), (SELECT COUNT(*) FROM _warehouse_map WHERE is_invalid = true);

    RAISE NOTICE '[%] Step 1 completed: Foreign key mappings created', clock_timestamp();

    -- ============================================================
    -- 2) CREATE VALID RECORDS SET
    -- ============================================================
    RAISE NOTICE '[%] Step 2: Creating valid records set...', clock_timestamp();

    CREATE TEMP TABLE _valid_records AS
    SELECT
        s.*,
        dm.department_id,
        rm.responsibility_id,
        wm.warehouse_id,
        ROW_NUMBER() OVER (ORDER BY s.created_at, s.id) as batch_order
    FROM staging_hsbg_hop_dong s
    LEFT JOIN _department_map dm ON dm.ma_don_vi = s.ma_don_vi
    LEFT JOIN _responsibility_map rm ON rm.trach_nhiem_ban_giao = s.trach_nhiem_ban_giao
    LEFT JOIN _warehouse_map wm ON wm.kho_vpbank = s.kho_vpbank
    WHERE s.job_id = job_id_in
      AND s.inserted_to_master = false
      AND s.validation_status = 'VALID'
      AND s.is_duplicate = false
      AND (dm.is_invalid = false OR s.ma_don_vi IS NULL)
      AND (rm.is_invalid = false OR s.trach_nhiem_ban_giao IS NULL)
      AND (wm.is_invalid = false OR s.kho_vpbank IS NULL);

    SELECT COUNT(*) INTO v_total_processed FROM _valid_records;

    RAISE NOTICE '[%] Step 2 completed: % valid records identified', clock_timestamp(), v_total_processed;

    -- ============================================================
    -- 3) DUPLICATE DETECTION (Against existing pdms_credit_casepdm)
    -- ============================================================
    RAISE NOTICE '[%] Step 3: Detecting duplicates against master data...', clock_timestamp();

    CREATE TEMP TABLE _duplicate_check AS
    WITH duplicate_logic AS (
        SELECT
            v.id as staging_id,
            v.loai_ho_so,
            v.so_hop_dong,
            v.so_cif_cccd_cmt,
            v.ngay_giai_ngan,
            v.ma_don_vi,
            v.department_id,
            CASE v.loai_ho_so
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
        FROM _valid_records v
    )
    SELECT
        dl.staging_id,
        dl.check_type,
        dl.loai_ho_so,
        dl.so_hop_dong,
        CASE
            -- Check type 1: contract_number + record_type + disbursement_date + delivery_method
            WHEN dl.check_type = 'contract_disbursement' THEN
                (SELECT p.id FROM pdms_credit_casepdm p
                 WHERE p.contract_number = dl.so_hop_dong
                   AND p.record_type = dl.loai_ho_so
                   AND p.disbursement_date = dl.ngay_giai_ngan
                   AND p.delivery_method = delivery_method_val
                 LIMIT 1)

            -- Check type 2: contract_number + record_type + cif_number + delivery_method
            WHEN dl.check_type = 'contract_cif' THEN
                (SELECT p.id FROM pdms_credit_casepdm p
                 WHERE p.contract_number = dl.so_hop_dong
                   AND p.record_type = dl.loai_ho_so
                   AND p.cif_number = dl.so_cif_cccd_cmt
                   AND p.delivery_method = delivery_method_val
                 LIMIT 1)

            -- Check type 3: contract_number + record_type + cif_number + department + disbursement_date + delivery_method
            WHEN dl.check_type = 'contract_cif_department_disbursement' THEN
                (SELECT p.id FROM pdms_credit_casepdm p
                 WHERE p.contract_number = dl.so_hop_dong
                   AND p.record_type = dl.loai_ho_so
                   AND p.cif_number = dl.so_cif_cccd_cmt
                   AND p.department_id = dl.department_id
                   AND p.disbursement_date = dl.ngay_giai_ngan
                   AND p.delivery_method = delivery_method_val
                 LIMIT 1)
            ELSE NULL
        END as existing_pdm_id
    FROM duplicate_logic dl;

    SELECT COUNT(*) INTO v_duplicate_count
    FROM _duplicate_check WHERE existing_pdm_id IS NOT NULL;

    RAISE NOTICE '[%] Step 3 completed: % duplicates found in master data', clock_timestamp(), v_duplicate_count;

    -- ============================================================
    -- 4) GROUP RECORDS BY BOX (ma_thung)
    -- ============================================================
    RAISE NOTICE '[%] Step 4: Grouping records by box...', clock_timestamp();

    -- Group records by ma_thung to create boxes
    CREATE TEMP TABLE _box_groups AS
    SELECT
        v.ma_thung,
        v.department_id,
        v.warehouse_id,
        v.responsibility_id,
        v.khu_vuc as area,
        v.hang as row_position,
        v.cot as column_position,
        v.tinh_trang_thung as state,
        v.trang_thai_thung as status,
        v.ngay_nhap_kho_vpbank as warehouse_import_date,
        v.ngay_chuyen_kho_crown as crown_transfer_date,
        v.loai_ho_so as document_type,
        v.phan_han_cap_td as storage_period,
        MIN(v.id) as first_record_id,
        COUNT(*) as record_count
    FROM _valid_records v
    LEFT JOIN _duplicate_check dc ON dc.staging_id = v.id
    WHERE dc.existing_pdm_id IS NULL  -- Exclude duplicates
      AND v.ma_thung IS NOT NULL
    GROUP BY
        v.ma_thung,
        v.department_id,
        v.warehouse_id,
        v.responsibility_id,
        v.khu_vuc,
        v.hang,
        v.cot,
        v.tinh_trang_thung,
        v.trang_thai_thung,
        v.ngay_nhap_kho_vpbank,
        v.ngay_chuyen_kho_crown,
        v.loai_ho_so,
        v.phan_han_cap_td;

    RAISE NOTICE '[%] Step 4 completed: % unique boxes identified', clock_timestamp(), (SELECT COUNT(*) FROM _box_groups);

    -- ============================================================
    -- 5) BATCH PROCESSING & MIGRATION
    -- ============================================================
    RAISE NOTICE '[%] Step 5: Starting batch migration...', clock_timestamp();

    -- Calculate total number of batches
    SELECT CEIL(COUNT(*)::DECIMAL / batch_size_in) INTO v_batch_count
    FROM _valid_records v
    LEFT JOIN _duplicate_check dc ON dc.staging_id = v.id
    WHERE dc.existing_pdm_id IS NULL;

    RAISE NOTICE '[%]   Total batches to process: %', clock_timestamp(), v_batch_count;

    -- Process each batch
    FOR v_batch_number IN 1..v_batch_count
    LOOP
        RAISE NOTICE '[%]   ========== Processing Batch %/% ==========', clock_timestamp(), v_batch_number, v_batch_count;

        -- Create temporary table for current batch
        DROP TABLE IF EXISTS _migration_batch;
        CREATE TEMP TABLE _migration_batch AS
        SELECT v.*
        FROM _valid_records v
        LEFT JOIN _duplicate_check dc ON dc.staging_id = v.id
        WHERE dc.existing_pdm_id IS NULL
          AND CEIL(v.batch_order::DECIMAL / batch_size_in) = v_batch_number;

        SELECT COUNT(*) INTO rec FROM _migration_batch;
        RAISE NOTICE '[%]     Batch %: Processing % records', clock_timestamp(), v_batch_number, rec;

        -- ============================================================
        -- 5.1) INSERT BOXES (pdms_box)
        -- ============================================================
        WITH box_inserts AS (
            INSERT INTO pdms_box (
                id,
                box_code,
                department_id,
                warehouse_id,
                responsibility_id,
                area,
                row_position,
                column_position,
                state,
                status,
                warehouse_import_date,
                crown_transfer_date,
                document_type,
                storage_period,
                is_migration,
                is_delete,
                created_date,
                last_modified_date,
                created_by,
                last_modified_by
            )
            SELECT DISTINCT
                gen_random_uuid() as id,
                bg.ma_thung as box_code,
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
                true as is_migration,
                false as is_delete,
                NOW() as created_date,
                NOW() as last_modified_date,
                'MIGRATION_SYSTEM' as created_by,
                'MIGRATION_SYSTEM' as last_modified_by
            FROM _box_groups bg
            WHERE bg.ma_thung IN (SELECT DISTINCT mb.ma_thung FROM _migration_batch mb WHERE mb.ma_thung IS NOT NULL)
              AND NOT EXISTS (
                  SELECT 1 FROM pdms_box b WHERE b.box_code = bg.ma_thung
              )
            ON CONFLICT (box_code) DO NOTHING
            RETURNING id, box_code
        )
        SELECT COUNT(*) INTO rec FROM box_inserts;

        RAISE NOTICE '[%]     Batch %: Inserted % new boxes', clock_timestamp(), v_batch_number, rec;

        -- ============================================================
        -- 5.2) INSERT CREDIT CASE PDM (pdms_credit_casepdm)
        -- ============================================================
        WITH pdm_inserts AS (
            INSERT INTO pdms_credit_casepdm (
                id,
                pdm_code,
                document_type,
                flow_type,
                status,
                delivery_method,
                contract_number,
                cif_number,
                customer_name,
                disbursement_date,
                credit_extension,
                record_type,
                department_id,
                responsibility_id,
                handover_date,
                customer_segmentation,
                product,
                nq_code,
                due_date,
                credit_term,
                dao_code,
                ts_code,
                rrt_id,
                note,
                expected_destruction_date,
                box_id,
                is_migration,
                created_date,
                last_modified_date,
                created_by,
                last_modified_by
            )
            SELECT
                gen_random_uuid() as id,
                FORMAT('PDMC-%08d', nextval('pdms_credit_casepdm_code_seq')) as pdm_code,
                COALESCE(mb.loai_ho_so, 'LD')::VARCHAR as document_type,
                'STANDARD'::VARCHAR as flow_type,
                COALESCE(mb.trang_thai_case_pdm, 'ACTIVE')::VARCHAR as status,
                delivery_method_val::VARCHAR as delivery_method,
                mb.so_hop_dong as contract_number,
                mb.so_cif_cccd_cmt as cif_number,
                mb.ten_khach_hang as customer_name,
                mb.ngay_giai_ngan as disbursement_date,
                mb.phan_han_cap_td::VARCHAR as credit_extension,
                mb.loai_ho_so::VARCHAR as record_type,
                mb.department_id,
                mb.responsibility_id,
                mb.ngay_ban_giao as handover_date,
                mb.phan_khach_khach_hang as customer_segmentation,
                mb.san_pham::VARCHAR as product,
                mb.ma_nq as nq_code,
                mb.ngay_den_han as due_date,
                mb.thoi_han_cap_td as credit_term,
                mb.ma_dao as dao_code,
                mb.ma_ts as ts_code,
                mb.rrt_id,
                mb.ghi_chu as note,
                mb.ngay_du_kien_tieu_huy as expected_destruction_date,
                (SELECT b.id FROM pdms_box b WHERE b.box_code = mb.ma_thung LIMIT 1) as box_id,
                true as is_migration,
                COALESCE(mb.created_at, NOW()) as created_date,
                NOW() as last_modified_date,
                'MIGRATION_SYSTEM' as created_by,
                'MIGRATION_SYSTEM' as last_modified_by
            FROM _migration_batch mb
            RETURNING id, pdm_code, contract_number, box_id
        )
        SELECT COUNT(*) INTO rec FROM pdm_inserts;

        RAISE NOTICE '[%]     Batch %: Inserted % PDM cases', clock_timestamp(), v_batch_number, rec;

        -- ============================================================
        -- 5.3) INSERT CREDIT DOCUMENTS (pdms_credit_document)
        -- ============================================================
        -- Group by ten_tap (folder name) to create credit documents
        WITH credit_doc_inserts AS (
            INSERT INTO pdms_credit_document (
                id,
                single_document,
                document_name,
                note,
                box_id,
                created_date,
                last_modified_date,
                created_by,
                last_modified_by
            )
            SELECT DISTINCT
                gen_random_uuid() as id,
                false as single_document,  -- Multiple cases can belong to one document
                COALESCE(mb.ten_tap, 'UNNAMED_FOLDER') as document_name,
                mb.ghi_chu as note,
                (SELECT b.id FROM pdms_box b WHERE b.box_code = mb.ma_thung LIMIT 1) as box_id,
                NOW() as created_date,
                NOW() as last_modified_date,
                'MIGRATION_SYSTEM' as created_by,
                'MIGRATION_SYSTEM' as last_modified_by
            FROM _migration_batch mb
            WHERE mb.ten_tap IS NOT NULL
            GROUP BY mb.ten_tap, mb.ghi_chu, mb.ma_thung
            RETURNING id, document_name, box_id
        )
        SELECT COUNT(*) INTO rec FROM credit_doc_inserts;

        RAISE NOTICE '[%]     Batch %: Inserted % credit documents', clock_timestamp(), v_batch_number, rec;

        -- ============================================================
        -- 5.4) LINK CREDIT DOCUMENTS TO PDM CASES (credit_document_pdm_case)
        -- ============================================================
        WITH link_inserts AS (
            INSERT INTO credit_document_pdm_case (
                credit_document_id,
                credit_pdm_case_id
            )
            SELECT DISTINCT
                cd.id as credit_document_id,
                pdm.id as credit_pdm_case_id
            FROM _migration_batch mb
            INNER JOIN pdms_credit_document cd
                ON cd.document_name = COALESCE(mb.ten_tap, 'UNNAMED_FOLDER')
                AND cd.box_id = (SELECT b.id FROM pdms_box b WHERE b.box_code = mb.ma_thung LIMIT 1)
            INNER JOIN pdms_credit_casepdm pdm
                ON pdm.contract_number = mb.so_hop_dong
                AND pdm.cif_number = mb.so_cif_cccd_cmt
                AND pdm.disbursement_date = mb.ngay_giai_ngan
                AND pdm.is_migration = true
            WHERE NOT EXISTS (
                SELECT 1 FROM credit_document_pdm_case cdpc
                WHERE cdpc.credit_document_id = cd.id
                  AND cdpc.credit_pdm_case_id = pdm.id
            )
            RETURNING credit_document_id, credit_pdm_case_id
        )
        SELECT COUNT(*) INTO rec FROM link_inserts;

        RAISE NOTICE '[%]     Batch %: Linked % document-case relationships', clock_timestamp(), v_batch_number, rec;

        -- ============================================================
        -- 5.5) UPDATE STAGING TABLE
        -- ============================================================
        UPDATE staging_hsbg_hop_dong s
        SET inserted_to_master = true,
            master_data_exists = true,
            inserted_at = NOW()
        WHERE s.id IN (SELECT id FROM _migration_batch);

        SELECT COUNT(*) INTO rec FROM _migration_batch;
        v_migrated_count := v_migrated_count + rec;

        RAISE NOTICE '[%]     Batch %: Updated % staging records as migrated', clock_timestamp(), v_batch_number, rec;
        RAISE NOTICE '[%]   ========== Batch %/% Completed ==========', clock_timestamp(), v_batch_number, v_batch_count;

    END LOOP;

    RAISE NOTICE '[%] Step 5 completed: All batches processed successfully', clock_timestamp();

    -- ============================================================
    -- 6) UPDATE DUPLICATE RECORDS IN STAGING
    -- ============================================================
    RAISE NOTICE '[%] Step 6: Marking duplicate records in staging...', clock_timestamp();

    UPDATE staging_hsbg_hop_dong s
    SET is_duplicate = true,
        duplicate_key = 'PDM_ID:' || dc.existing_pdm_id::text,
        master_data_exists = true
    FROM _duplicate_check dc
    WHERE s.id = dc.staging_id
      AND dc.existing_pdm_id IS NOT NULL;

    RAISE NOTICE '[%] Step 6 completed: % duplicate records marked', clock_timestamp(), v_duplicate_count;

    -- ============================================================
    -- 7) COLLECT WARNINGS
    -- ============================================================
    RAISE NOTICE '[%] Step 7: Collecting migration warnings...', clock_timestamp();

    -- Create permanent warnings table if not exists
    CREATE TABLE IF NOT EXISTS migration_warnings (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        job_id UUID NOT NULL,
        migration_type TEXT NOT NULL,
        warning_type TEXT NOT NULL,
        message TEXT,
        detail JSONB,
        created_at TIMESTAMP DEFAULT NOW()
    );

    -- Warning 1: Duplicate records found
    IF v_duplicate_count > 0 THEN
        INSERT INTO _migration_warnings (warning_type, message, detail)
        SELECT
            'DUPLICATE_RECORDS',
            FORMAT('Found %s duplicate records already existing in pdms_credit_casepdm', v_duplicate_count),
            jsonb_build_object(
                'job_id', job_id_in,
                'total_duplicates', v_duplicate_count,
                'check_type_breakdown', (
                    SELECT jsonb_object_agg(check_type, cnt)
                    FROM (
                        SELECT check_type, COUNT(*) as cnt
                        FROM _duplicate_check
                        WHERE existing_pdm_id IS NOT NULL
                        GROUP BY check_type
                    ) x
                ),
                'sample_duplicates', (
                    SELECT jsonb_agg(
                        jsonb_build_object(
                            'staging_id', staging_id,
                            'check_type', check_type,
                            'contract_number', so_hop_dong,
                            'existing_pdm_id', existing_pdm_id
                        )
                    )
                    FROM (
                        SELECT * FROM _duplicate_check
                        WHERE existing_pdm_id IS NOT NULL
                        LIMIT 10
                    ) sample
                )
            );
    END IF;

    -- Warning 2: Invalid foreign key references
    INSERT INTO _migration_warnings (warning_type, message, detail)
    SELECT
        'INVALID_FOREIGN_KEYS',
        'Found invalid foreign key references that prevented some records from migrating',
        jsonb_build_object(
            'job_id', job_id_in,
            'invalid_departments', (
                SELECT jsonb_agg(DISTINCT ma_don_vi)
                FROM _department_map WHERE is_invalid = true
            ),
            'invalid_responsibilities', (
                SELECT jsonb_agg(DISTINCT trach_nhiem_ban_giao)
                FROM _responsibility_map WHERE is_invalid = true
            ),
            'invalid_warehouses', (
                SELECT jsonb_agg(DISTINCT kho_vpbank)
                FROM _warehouse_map WHERE is_invalid = true
            )
        )
    WHERE EXISTS (
        SELECT 1 FROM _department_map WHERE is_invalid = true
        UNION ALL
        SELECT 1 FROM _responsibility_map WHERE is_invalid = true
        UNION ALL
        SELECT 1 FROM _warehouse_map WHERE is_invalid = true
    );

    -- Warning 3: Records with validation errors (skipped)
    SELECT COUNT(*) INTO v_error_count
    FROM staging_hsbg_hop_dong
    WHERE job_id = job_id_in
      AND inserted_to_master = false
      AND validation_status = 'INVALID';

    IF v_error_count > 0 THEN
        INSERT INTO _migration_warnings (warning_type, message, detail)
        SELECT
            'VALIDATION_ERRORS_SKIPPED',
            FORMAT('%s records skipped due to validation errors', v_error_count),
            jsonb_build_object(
                'job_id', job_id_in,
                'total_skipped', v_error_count,
                'sample_records', (
                    SELECT jsonb_agg(
                        jsonb_build_object(
                            'staging_id', id,
                            'contract_number', so_hop_dong,
                            'validation_status', validation_status
                        )
                    )
                    FROM (
                        SELECT * FROM staging_hsbg_hop_dong
                        WHERE job_id = job_id_in
                          AND inserted_to_master = false
                          AND validation_status = 'INVALID'
                        LIMIT 5
                    ) sample
                )
            );
    END IF;

    -- Save warnings to permanent table
    INSERT INTO migration_warnings (job_id, migration_type, warning_type, message, detail)
    SELECT job_id_in, 'HSBG_HOP_DONG', warning_type, message, detail
    FROM _migration_warnings;

    SELECT COUNT(*) INTO v_warning_count FROM _migration_warnings;

    RAISE NOTICE '[%] Step 7 completed: % warnings logged', clock_timestamp(), v_warning_count;

    -- ============================================================
    -- 8) PRINT WARNING DETAILS
    -- ============================================================
    IF v_warning_count > 0 THEN
        RAISE NOTICE '[%] ======================================== WARNINGS ========================================', clock_timestamp();
        FOR rec_warn IN
            SELECT warning_type, message, detail
            FROM _migration_warnings
        LOOP
            RAISE NOTICE '[%]   Warning Type: %', clock_timestamp(), rec_warn.warning_type;
            RAISE NOTICE '[%]   Message: %', clock_timestamp(), rec_warn.message;
            RAISE NOTICE '[%]   Details: %', clock_timestamp(), rec_warn.detail::text;
            RAISE NOTICE '[%]   ---', clock_timestamp();
        END LOOP;
        RAISE NOTICE '[%] ========================================================================================', clock_timestamp();
    END IF;

    -- ============================================================
    -- 9) CLEANUP TEMPORARY TABLES
    -- ============================================================
    DROP TABLE IF EXISTS _migration_warnings;
    DROP TABLE IF EXISTS _valid_records;
    DROP TABLE IF EXISTS _duplicate_check;
    DROP TABLE IF EXISTS _department_map;
    DROP TABLE IF EXISTS _responsibility_map;
    DROP TABLE IF EXISTS _warehouse_map;
    DROP TABLE IF EXISTS _migration_batch;
    DROP TABLE IF EXISTS _box_groups;

    -- ============================================================
    -- 10) FINAL SUMMARY
    -- ============================================================
    RAISE NOTICE '[%] ======================================== MIGRATION SUMMARY ========================================', clock_timestamp();
    RAISE NOTICE '[%] Migration Type: HSBG_HOP_DONG', clock_timestamp();
    RAISE NOTICE '[%] Job ID: %', clock_timestamp(), job_id_in;
    RAISE NOTICE '[%] ---', clock_timestamp();
    RAISE NOTICE '[%] Total Valid Records: %', clock_timestamp(), v_total_processed;
    RAISE NOTICE '[%] Successfully Migrated: %', clock_timestamp(), v_migrated_count;
    RAISE NOTICE '[%] Duplicates Found: %', clock_timestamp(), v_duplicate_count;
    RAISE NOTICE '[%] Validation Errors (Skipped): %', clock_timestamp(), v_error_count;
    RAISE NOTICE '[%] Warnings Logged: %', clock_timestamp(), v_warning_count;
    RAISE NOTICE '[%] ---', clock_timestamp();
    RAISE NOTICE '[%] Batch Size: %', clock_timestamp(), batch_size_in;
    RAISE NOTICE '[%] Total Batches: %', clock_timestamp(), v_batch_count;
    RAISE NOTICE '[%] Duration: %', clock_timestamp(), clock_timestamp() - v_start_time;
    RAISE NOTICE '[%] ====================================================================================================', clock_timestamp();

    RETURN QUERY SELECT
        v_total_processed,
        v_migrated_count,
        v_duplicate_count,
        v_error_count,
        v_warning_count;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '[%] ❌ MIGRATION FAILED for HSBG_HOP_DONG - Job ID: % | Error: % | SQLSTATE: %',
            clock_timestamp(), job_id_in, SQLERRM, SQLSTATE;
END;
$$;

-- Grant permissions
COMMENT ON FUNCTION migrate_hsbg_hop_dong(UUID, INTEGER) IS
'Migrates validated HSBG_HOP_DONG records to pdms_box, pdms_credit_casepdm, pdms_credit_document with proper entity relationships';

-- Example usage:
-- SELECT * FROM migrate_hsbg_hop_dong('your-job-id-here'::UUID, 1000);
