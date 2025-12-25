package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.entity.StagingHSBGCif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for StagingHSBGCif
 */
@Repository
public interface StagingHSBGCifRepository extends JpaRepository<StagingHSBGCif, UUID> {
    
    @Query("SELECT s FROM StagingHSBGCif s WHERE s.sheetId = :sheetId AND s.validationStatus = 'VALID' AND s.insertedToMaster = false ORDER BY s.rowNumber")
    List<StagingHSBGCif> findValidRecordsForInsert(@Param("sheetId") UUID sheetId, org.springframework.data.domain.Pageable pageable);
    
    @Modifying
    @Query("UPDATE StagingHSBGCif s SET s.insertedToMaster = true, s.insertedAt = :now WHERE s.id IN :ids")
    void markAsInserted(@Param("ids") List<UUID> ids, @Param("now") Instant now);
    
    @Query("SELECT COUNT(s) FROM StagingHSBGCif s WHERE s.sheetId = :sheetId AND s.validationStatus = 'VALID'")
    long countValidRecords(@Param("sheetId") UUID sheetId);
    
    @Query("SELECT COUNT(s) FROM StagingHSBGCif s WHERE s.sheetId = :sheetId AND s.validationStatus = 'INVALID'")
    long countInvalidRecords(@Param("sheetId") UUID sheetId);
}

