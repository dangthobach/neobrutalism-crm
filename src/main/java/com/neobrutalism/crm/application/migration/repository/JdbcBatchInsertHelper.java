package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.entity.StagingHSBGCif;
import com.neobrutalism.crm.application.migration.entity.StagingHSBGHopDong;
import com.neobrutalism.crm.application.migration.entity.StagingHSBGTap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC Batch Insert Helper
 * 
 * Provides high-performance batch inserts using raw JDBC
 * 5-10x faster than JPA saveAll() for large datasets
 * 
 * Performance improvements:
 * - Direct JDBC PreparedStatement batching
 * - No Hibernate overhead (dirty checking, caching, etc.)
 * - Optimized for write-heavy operations
 * - Manual commit control for better throughput
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcBatchInsertHelper {

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BATCH_COMMIT_SIZE = 500;

    /**
     * Batch insert for StagingHSBGHopDong
     * Uses JDBC batch with manual commits every 500 records
     * 
     * @param records List of staging records to insert
     * @return Number of records inserted
     */
    @Transactional
    public int batchInsertHopDong(List<StagingHSBGHopDong> records) {
        if (records.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT INTO staging_hsbg_hop_dong
            (id, job_id, sheet_id, row_number, warehouse_vpbank, unit_code,
             delivery_responsibility, contract_number, volume_name, volume_quantity,
             customer_cif_cccd_cmt, customer_name, customer_segment,
             required_delivery_date, delivery_date, disbursement_date, due_date,
             document_type, document_flow, credit_term_category,
             expected_destruction_date, product, pdm_case_status, notes,
             box_code, vpbank_warehouse_entry_date, crown_warehouse_transfer_date,
             area, row, column, box_condition, box_status, credit_term_months,
             dao_code, ts_code, rrt_id, nq_code, duplicate_key,
             validation_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        Session session = entityManager.unwrap(Session.class);
        final int[] insertedCount = {0};

        session.doWork(connection -> {
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int count = 0;

                for (StagingHSBGHopDong record : records) {
                    setPreparedStatementHopDong(pstmt, record);
                    pstmt.addBatch();

                    // Commit every BATCH_COMMIT_SIZE records
                    if (++count % BATCH_COMMIT_SIZE == 0) {
                        pstmt.executeBatch();
                        connection.commit();
                        insertedCount[0] += BATCH_COMMIT_SIZE;
                        log.trace("Committed {} HopDong records", count);
                    }
                }

                // Execute remaining records
                int[] results = pstmt.executeBatch();
                connection.commit();
                insertedCount[0] += results.length;

                log.debug("Batch inserted {} HopDong records using JDBC", insertedCount[0]);

            } catch (SQLException e) {
                connection.rollback();
                log.error("Failed to batch insert HopDong records", e);
                throw new RuntimeException("JDBC batch insert failed for HopDong", e);
            }
        });

        return insertedCount[0];
    }

    /**
     * Batch insert for StagingHSBGCif
     */
    @Transactional
    public int batchInsertCif(List<StagingHSBGCif> records) {
        if (records.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT INTO staging_hsbg_cif
            (id, job_id, sheet_id, row_number, warehouse_vpbank, unit_code,
             delivery_responsibility, customer_cif, customer_name, volume_name,
             volume_quantity, customer_segment, required_delivery_date, delivery_date,
             disbursement_date, document_type, document_flow, credit_term_category,
             product, pdm_case_status, notes, nq_code, box_code,
             vpbank_warehouse_entry_date, crown_warehouse_transfer_date,
             area, row, column, box_condition, box_status, duplicate_key,
             validation_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        Session session = entityManager.unwrap(Session.class);
        final int[] insertedCount = {0};

        session.doWork(connection -> {
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int count = 0;

                for (StagingHSBGCif record : records) {
                    setPreparedStatementCif(pstmt, record);
                    pstmt.addBatch();

                    if (++count % BATCH_COMMIT_SIZE == 0) {
                        pstmt.executeBatch();
                        connection.commit();
                        insertedCount[0] += BATCH_COMMIT_SIZE;
                        log.trace("Committed {} Cif records", count);
                    }
                }

                int[] results = pstmt.executeBatch();
                connection.commit();
                insertedCount[0] += results.length;

                log.debug("Batch inserted {} Cif records using JDBC", insertedCount[0]);

            } catch (SQLException e) {
                connection.rollback();
                log.error("Failed to batch insert Cif records", e);
                throw new RuntimeException("JDBC batch insert failed for Cif", e);
            }
        });

        return insertedCount[0];
    }

    /**
     * Batch insert for StagingHSBGTap
     */
    @Transactional
    public int batchInsertTap(List<StagingHSBGTap> records) {
        if (records.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT INTO staging_hsbg_tap
            (id, job_id, sheet_id, row_number, warehouse_vpbank, unit_code,
             delivery_responsibility, occurrence_month, volume_name, volume_quantity,
             required_delivery_date, delivery_date, document_type, document_flow,
             credit_term_category, expected_destruction_date, product, pdm_case_status,
             notes, box_code, vpbank_warehouse_entry_date, crown_warehouse_transfer_date,
             area, row, column, box_condition, box_status, duplicate_key,
             validation_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        Session session = entityManager.unwrap(Session.class);
        final int[] insertedCount = {0};

        session.doWork(connection -> {
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int count = 0;

                for (StagingHSBGTap record : records) {
                    setPreparedStatementTap(pstmt, record);
                    pstmt.addBatch();

                    if (++count % BATCH_COMMIT_SIZE == 0) {
                        pstmt.executeBatch();
                        connection.commit();
                        insertedCount[0] += BATCH_COMMIT_SIZE;
                        log.trace("Committed {} Tap records", count);
                    }
                }

                int[] results = pstmt.executeBatch();
                connection.commit();
                insertedCount[0] += results.length;

                log.debug("Batch inserted {} Tap records using JDBC", insertedCount[0]);

            } catch (SQLException e) {
                connection.rollback();
                log.error("Failed to batch insert Tap records", e);
                throw new RuntimeException("JDBC batch insert failed for Tap", e);
            }
        });

        return insertedCount[0];
    }

    // ========== PreparedStatement Setters ==========

    private void setPreparedStatementHopDong(PreparedStatement pstmt, StagingHSBGHopDong record) throws SQLException {
        int idx = 1;
        pstmt.setObject(idx++, record.getId());
        pstmt.setObject(idx++, record.getJobId());
        pstmt.setObject(idx++, record.getSheetId());
        pstmt.setLong(idx++, record.getRowNumber());
        pstmt.setString(idx++, record.getWarehouseVpbank());
        pstmt.setString(idx++, record.getUnitCode());
        pstmt.setString(idx++, record.getDeliveryResponsibility());
        pstmt.setString(idx++, record.getContractNumber());
        pstmt.setString(idx++, record.getVolumeName());
        pstmt.setObject(idx++, record.getVolumeQuantity());
        pstmt.setString(idx++, record.getCustomerCifCccdCmt());
        pstmt.setString(idx++, record.getCustomerName());
        pstmt.setString(idx++, record.getCustomerSegment());
        pstmt.setObject(idx++, record.getRequiredDeliveryDate());
        pstmt.setObject(idx++, record.getDeliveryDate());
        pstmt.setObject(idx++, record.getDisbursementDate());
        pstmt.setObject(idx++, record.getDueDate());
        pstmt.setString(idx++, record.getDocumentType());
        pstmt.setString(idx++, record.getDocumentFlow());
        pstmt.setString(idx++, record.getCreditTermCategory());
        pstmt.setObject(idx++, record.getExpectedDestructionDate());
        pstmt.setString(idx++, record.getProduct());
        pstmt.setString(idx++, record.getPdmCaseStatus());
        pstmt.setString(idx++, record.getNotes());
        pstmt.setString(idx++, record.getBoxCode());
        pstmt.setObject(idx++, record.getVpbankWarehouseEntryDate());
        pstmt.setObject(idx++, record.getCrownWarehouseTransferDate());
        pstmt.setString(idx++, record.getArea());
        pstmt.setString(idx++, record.getRow());
        pstmt.setString(idx++, record.getColumn());
        pstmt.setString(idx++, record.getBoxCondition());
        pstmt.setString(idx++, record.getBoxStatus());
        pstmt.setObject(idx++, record.getCreditTermMonths());
        pstmt.setString(idx++, record.getDaoCode());
        pstmt.setString(idx++, record.getTsCode());
        pstmt.setString(idx++, record.getRrtId());
        pstmt.setString(idx++, record.getNqCode());
        pstmt.setString(idx++, record.getDuplicateKey());
        pstmt.setString(idx++, record.getValidationStatus());
    }

    private void setPreparedStatementCif(PreparedStatement pstmt, StagingHSBGCif record) throws SQLException {
        int idx = 1;
        pstmt.setObject(idx++, record.getId());
        pstmt.setObject(idx++, record.getJobId());
        pstmt.setObject(idx++, record.getSheetId());
        pstmt.setLong(idx++, record.getRowNumber());
        pstmt.setString(idx++, record.getWarehouseVpbank());
        pstmt.setString(idx++, record.getUnitCode());
        pstmt.setString(idx++, record.getDeliveryResponsibility());
        pstmt.setString(idx++, record.getCustomerCif());
        pstmt.setString(idx++, record.getCustomerName());
        pstmt.setString(idx++, record.getVolumeName());
        pstmt.setObject(idx++, record.getVolumeQuantity());
        pstmt.setString(idx++, record.getCustomerSegment());
        pstmt.setObject(idx++, record.getRequiredDeliveryDate());
        pstmt.setObject(idx++, record.getDeliveryDate());
        pstmt.setObject(idx++, record.getDisbursementDate());
        pstmt.setString(idx++, record.getDocumentType());
        pstmt.setString(idx++, record.getDocumentFlow());
        pstmt.setString(idx++, record.getCreditTermCategory());
        pstmt.setString(idx++, record.getProduct());
        pstmt.setString(idx++, record.getPdmCaseStatus());
        pstmt.setString(idx++, record.getNotes());
        pstmt.setString(idx++, record.getNqCode());
        pstmt.setString(idx++, record.getBoxCode());
        pstmt.setObject(idx++, record.getVpbankWarehouseEntryDate());
        pstmt.setObject(idx++, record.getCrownWarehouseTransferDate());
        pstmt.setString(idx++, record.getArea());
        pstmt.setString(idx++, record.getRow());
        pstmt.setString(idx++, record.getColumn());
        pstmt.setString(idx++, record.getBoxCondition());
        pstmt.setString(idx++, record.getBoxStatus());
        pstmt.setString(idx++, record.getDuplicateKey());
        pstmt.setString(idx++, record.getValidationStatus());
    }

    private void setPreparedStatementTap(PreparedStatement pstmt, StagingHSBGTap record) throws SQLException {
        int idx = 1;
        pstmt.setObject(idx++, record.getId());
        pstmt.setObject(idx++, record.getJobId());
        pstmt.setObject(idx++, record.getSheetId());
        pstmt.setLong(idx++, record.getRowNumber());
        pstmt.setString(idx++, record.getWarehouseVpbank());
        pstmt.setString(idx++, record.getUnitCode());
        pstmt.setString(idx++, record.getDeliveryResponsibility());
        pstmt.setObject(idx++, record.getOccurrenceMonth());
        pstmt.setString(idx++, record.getVolumeName());
        pstmt.setObject(idx++, record.getVolumeQuantity());
        pstmt.setObject(idx++, record.getRequiredDeliveryDate());
        pstmt.setObject(idx++, record.getDeliveryDate());
        pstmt.setString(idx++, record.getDocumentType());
        pstmt.setString(idx++, record.getDocumentFlow());
        pstmt.setString(idx++, record.getCreditTermCategory());
        pstmt.setObject(idx++, record.getExpectedDestructionDate());
        pstmt.setString(idx++, record.getProduct());
        pstmt.setString(idx++, record.getPdmCaseStatus());
        pstmt.setString(idx++, record.getNotes());
        pstmt.setString(idx++, record.getBoxCode());
        pstmt.setObject(idx++, record.getVpbankWarehouseEntryDate());
        pstmt.setObject(idx++, record.getCrownWarehouseTransferDate());
        pstmt.setString(idx++, record.getArea());
        pstmt.setString(idx++, record.getRow());
        pstmt.setString(idx++, record.getColumn());
        pstmt.setString(idx++, record.getBoxCondition());
        pstmt.setString(idx++, record.getBoxStatus());
        pstmt.setString(idx++, record.getDuplicateKey());
        pstmt.setString(idx++, record.getValidationStatus());
    }
}
