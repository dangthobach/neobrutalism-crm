package com.neobrutalism.crm.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for audit logs
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by tenant ID
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find audit logs by entity type and ID
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntityTypeAndEntityId(
        @Param("entityType") String entityType, 
        @Param("entityId") UUID entityId
    );

    /**
     * Find audit logs by tenant and entity
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByTenantAndEntity(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") String entityType, 
        @Param("entityId") UUID entityId
    );

    /**
     * Find audit logs by user ID
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find audit logs by action
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.action = :action ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantIdAndAction(
        @Param("tenantId") UUID tenantId,
        @Param("action") AuditAction action, 
        Pageable pageable
    );

    /**
     * Find audit logs by date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantIdAndCreatedAtBetween(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Find failed operations
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.success = false ORDER BY a.createdAt DESC")
    Page<AuditLog> findFailedOperations(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Count audit logs by tenant
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Count audit logs by entity
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId AND a.entityType = :entityType AND a.entityId = :entityId")
    long countByTenantAndEntity(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId
    );

    /**
     * Find recent activities for dashboard
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.success = true ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentActivities(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Search audit logs by entity type and action
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.entityType = :entityType AND a.action = :action ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantEntityAndAction(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") String entityType,
        @Param("action") AuditAction action,
        Pageable pageable
    );

    /**
     * Find user activities
     */
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.userId = :userId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findUserActivities(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId,
        @Param("since") Instant since
    );
}
