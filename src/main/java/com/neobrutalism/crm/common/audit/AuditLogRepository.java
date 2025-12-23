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
 * Repository for application audit logs
 * Separate from event-sourcing audit logs in common.event package
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by tenant and entity
     */
    @Query("SELECT a FROM ApplicationAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findByTenantAndEntity(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId);

    /**
     * Find all audit logs for a tenant
     */
    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find audit logs by user
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find audit logs by tenant and action
     */
    Page<AuditLog> findByTenantIdAndAction(
            UUID tenantId,
            AuditAction action,
            Pageable pageable);

    /**
     * Find audit logs in date range for tenant
     */
    Page<AuditLog> findByTenantIdAndCreatedAtBetween(
            UUID tenantId,
            Instant startDate,
            Instant endDate,
            Pageable pageable);

    /**
     * Find failed operations (where success = false)
     */
    @Query("SELECT a FROM ApplicationAuditLog a WHERE a.tenantId = :tenantId AND a.success = false " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findFailedOperations(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find recent activities for tenant
     */
    @Query("SELECT a FROM ApplicationAuditLog a WHERE a.tenantId = :tenantId " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findRecentActivities(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Count audit logs by tenant and entity
     */
    @Query("SELECT COUNT(a) FROM ApplicationAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.entityType = :entityType AND a.entityId = :entityId")
    long countByTenantAndEntity(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId);

    /**
     * Find user activities since a specific time
     */
    @Query("SELECT a FROM ApplicationAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.userId = :userId AND a.createdAt >= :since " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findUserActivities(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("since") Instant since);

    /**
     * Find audit logs by tenant, entity type, and action
     */
    @Query("SELECT a FROM ApplicationAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.entityType = :entityType AND a.action = :action " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantEntityAndAction(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("action") AuditAction action,
            Pageable pageable);
}
