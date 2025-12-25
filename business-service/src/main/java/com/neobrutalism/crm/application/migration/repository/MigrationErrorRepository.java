package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for querying migration errors
 * Note: This uses native queries to access excel_migration_errors table
 */
@Repository
public interface MigrationErrorRepository extends JpaRepository<MigrationSheet, UUID> {
    
    @Query(value = """
        SELECT 
            e.id::text,
            e.row_number,
            e.batch_number,
            e.error_code,
            e.error_message,
            e.validation_rule,
            e.error_data,
            e.created_at
        FROM excel_migration_errors e
        WHERE e.sheet_id = :sheetId
        ORDER BY e.row_number
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findErrorsBySheetId(@Param("sheetId") UUID sheetId, 
                                       @Param("limit") int limit, 
                                       @Param("offset") int offset);
    
    @Query(value = """
        SELECT COUNT(*) 
        FROM excel_migration_errors e
        WHERE e.sheet_id = :sheetId
        """, nativeQuery = true)
    long countErrorsBySheetId(@Param("sheetId") UUID sheetId);
    
    @Query(value = """
        SELECT 
            e.sheet_id::text,
            e.id::text,
            e.row_number,
            e.batch_number,
            e.error_code,
            e.error_message,
            e.validation_rule,
            e.error_data,
            e.created_at
        FROM excel_migration_errors e
        INNER JOIN excel_migration_sheets s ON e.sheet_id = s.id
        WHERE s.job_id = :jobId
        ORDER BY s.sheet_name, e.row_number
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findErrorsByJobId(@Param("jobId") UUID jobId,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);
    
    @Query(value = """
        SELECT COUNT(*) 
        FROM excel_migration_errors e
        INNER JOIN excel_migration_sheets s ON e.sheet_id = s.id
        WHERE s.job_id = :jobId
        """, nativeQuery = true)
    long countErrorsByJobId(@Param("jobId") UUID jobId);
}

