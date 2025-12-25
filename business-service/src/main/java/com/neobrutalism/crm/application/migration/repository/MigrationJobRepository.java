package com.neobrutalism.crm.application.migration.repository;

import com.neobrutalism.crm.application.migration.model.MigrationJob;
import com.neobrutalism.crm.application.migration.model.MigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for MigrationJob
 */
@Repository
public interface MigrationJobRepository extends JpaRepository<MigrationJob, UUID> {
    
    boolean existsByFileHash(String fileHash);
    
    List<MigrationJob> findByStatus(MigrationStatus status);
    
    @Query("SELECT j FROM MigrationJob j WHERE j.status IN :statuses ORDER BY j.createdAt DESC")
    List<MigrationJob> findByStatusIn(@Param("statuses") List<MigrationStatus> statuses);
}

