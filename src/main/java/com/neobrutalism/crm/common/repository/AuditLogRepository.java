package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.event.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for audit logs
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for an entity
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            String entityType, String entityId, Pageable pageable);

    /**
     * Find all audit logs for an entity without pagination
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            String entityType, String entityId);

    /**
     * Find audit logs by action
     */
    Page<AuditLog> findByActionOrderByChangedAtDesc(String action, Pageable pageable);

    /**
     * Find audit logs by user
     */
    Page<AuditLog> findByChangedByOrderByChangedAtDesc(String changedBy, Pageable pageable);

    /**
     * Find audit logs in date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.changedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.changedAt DESC")
    Page<AuditLog> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Find audit logs for entity and action
     */
    List<AuditLog> findByEntityTypeAndEntityIdAndActionOrderByChangedAtDesc(
            String entityType, String entityId, String action);
}
