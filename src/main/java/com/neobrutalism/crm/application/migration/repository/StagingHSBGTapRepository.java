package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.entity.StagingHSBGTap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for StagingHSBGTap
 */
@Repository
public interface StagingHSBGTapRepository extends JpaRepository<StagingHSBGTap, UUID> {
    
    @Query("SELECT s FROM StagingHSBGTap s WHERE s.sheetId = :sheetId AND s.validationStatus = 'VALID' AND s.insertedToMaster = false ORDER BY s.rowNumber")
    List<StagingHSBGTap> findValidRecordsForInsert(@Param("sheetId") UUID sheetId, org.springframework.data.domain.Pageable pageable);
    
    @Modifying
    @Query("UPDATE StagingHSBGTap s SET s.insertedToMaster = true, s.insertedAt = CURRENT_TIMESTAMP WHERE s.id IN :ids")
    void markAsInserted(@Param("ids") List<UUID> ids);
    
    @Query("SELECT COUNT(s) FROM StagingHSBGTap s WHERE s.sheetId = :sheetId AND s.validationStatus = 'VALID'")
    long countValidRecords(@Param("sheetId") UUID sheetId);
    
    @Query("SELECT COUNT(s) FROM StagingHSBGTap s WHERE s.sheetId = :sheetId AND s.validationStatus = 'INVALID'")
    long countInvalidRecords(@Param("sheetId") UUID sheetId);
}

