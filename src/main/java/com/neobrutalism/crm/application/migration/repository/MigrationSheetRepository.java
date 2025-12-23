package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.model.MigrationSheet;
import com.neobrutalism.crm.application.migration.model.SheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MigrationSheet
 */
@Repository
public interface MigrationSheetRepository extends JpaRepository<MigrationSheet, UUID> {
    
    List<MigrationSheet> findByJobId(UUID jobId);
    
    Optional<MigrationSheet> findByJobIdAndSheetName(UUID jobId, String sheetName);
    
    List<MigrationSheet> findByStatus(SheetStatus status);
    
    @Query("SELECT s FROM MigrationSheet s WHERE s.status = 'PROCESSING' AND s.lastHeartbeat < :threshold")
    List<MigrationSheet> findStuckSheets(@Param("threshold") Instant threshold);
}

