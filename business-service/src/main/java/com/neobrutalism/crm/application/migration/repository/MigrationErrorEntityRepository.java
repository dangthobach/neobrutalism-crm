package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.entity.MigrationError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for MigrationError entity
 */
@Repository
public interface MigrationErrorEntityRepository extends JpaRepository<MigrationError, UUID> {
    
    List<MigrationError> findBySheetIdOrderByRowNumber(UUID sheetId);
    
    @Query("SELECT e FROM MigrationError e WHERE e.sheetId = :sheetId ORDER BY e.rowNumber")
    List<MigrationError> findErrorsBySheetId(@Param("sheetId") UUID sheetId);
    
    @Query("SELECT COUNT(e) FROM MigrationError e WHERE e.sheetId = :sheetId")
    long countBySheetId(@Param("sheetId") UUID sheetId);
    
    @Query("""
        SELECT e FROM MigrationError e 
        INNER JOIN MigrationSheet s ON e.sheetId = s.id 
        WHERE s.jobId = :jobId 
        ORDER BY s.sheetName, e.rowNumber
        """)
    List<MigrationError> findErrorsByJobId(@Param("jobId") UUID jobId);
    
    @Query("""
        SELECT COUNT(e) FROM MigrationError e 
        INNER JOIN MigrationSheet s ON e.sheetId = s.id 
        WHERE s.jobId = :jobId
        """)
    long countByJobId(@Param("jobId") UUID jobId);
}

