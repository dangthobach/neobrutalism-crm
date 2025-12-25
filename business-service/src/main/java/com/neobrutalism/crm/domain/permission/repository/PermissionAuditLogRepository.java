package com.neobrutalism.crm.domain.permission.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.permission.model.PermissionActionType;
import com.neobrutalism.crm.domain.permission.model.PermissionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Permission Audit Logs
 */
@Repository
public interface PermissionAuditLogRepository extends BaseRepository<PermissionAuditLog> {

    /**
     * Find audit logs by target user
     */
    Page<PermissionAuditLog> findByTargetUserIdOrderByChangedAtDesc(UUID targetUserId, Pageable pageable);

    /**
     * Find audit logs by action type
     */
    Page<PermissionAuditLog> findByActionTypeOrderByChangedAtDesc(PermissionActionType actionType, Pageable pageable);

    /**
     * Find audit logs by changed by user
     */
    Page<PermissionAuditLog> findByChangedByUserIdOrderByChangedAtDesc(UUID changedByUserId, Pageable pageable);

    /**
     * Find audit logs within date range
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE p.changedAt BETWEEN :startDate AND :endDate ORDER BY p.changedAt DESC")
    Page<PermissionAuditLog> findByDateRange(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Find audit logs by role code
     */
    Page<PermissionAuditLog> findByTargetRoleCodeOrderByChangedAtDesc(String roleCode, Pageable pageable);

    /**
     * Find audit logs by resource and action
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE p.resource = :resource AND p.action = :action ORDER BY p.changedAt DESC")
    Page<PermissionAuditLog> findByResourceAndAction(
        @Param("resource") String resource,
        @Param("action") String action,
        Pageable pageable
    );

    /**
     * Find failed permission attempts
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE p.success = false ORDER BY p.changedAt DESC")
    Page<PermissionAuditLog> findFailedAttempts(Pageable pageable);

    /**
     * Find critical security events
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE p.actionType IN ('UNAUTHORIZED_ACCESS_ATTEMPT', 'PERMISSION_ESCALATION_ATTEMPT', 'DATA_SCOPE_CHANGED') ORDER BY p.changedAt DESC")
    Page<PermissionAuditLog> findCriticalEvents(Pageable pageable);

    /**
     * Count audit logs by action type within date range
     */
    @Query("SELECT COUNT(p) FROM PermissionAuditLog p WHERE p.actionType = :actionType AND p.changedAt BETWEEN :startDate AND :endDate")
    long countByActionTypeAndDateRange(
        @Param("actionType") PermissionActionType actionType,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find recent audit logs for a user
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE p.targetUserId = :userId ORDER BY p.changedAt DESC")
    List<PermissionAuditLog> findRecentByTargetUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find audit logs by session ID (for correlating related changes)
     */
    List<PermissionAuditLog> findBySessionIdOrderByChangedAtAsc(String sessionId);

    /**
     * Find audit logs by tenant
     */
    Page<PermissionAuditLog> findByTenantIdOrderByChangedAtDesc(String tenantId, Pageable pageable);

    /**
     * Find audit logs by organization
     */
    Page<PermissionAuditLog> findByOrganizationIdOrderByChangedAtDesc(UUID organizationId, Pageable pageable);

    /**
     * Count recent failed attempts for a user (for security monitoring)
     */
    @Query("SELECT COUNT(p) FROM PermissionAuditLog p WHERE p.targetUserId = :userId AND p.success = false AND p.changedAt > :since")
    long countRecentFailedAttempts(
        @Param("userId") UUID userId,
        @Param("since") Instant since
    );

    /**
     * Get audit summary statistics
     */
    @Query("SELECT p.actionType, COUNT(p) FROM PermissionAuditLog p WHERE p.changedAt BETWEEN :startDate AND :endDate GROUP BY p.actionType")
    List<Object[]> getAuditStatistics(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find audit logs by target user and action types
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE p.targetUserId = :userId AND p.actionType IN :actionTypes ORDER BY p.changedAt DESC")
    Page<PermissionAuditLog> findByTargetUserAndActionTypes(
        @Param("userId") UUID userId,
        @Param("actionTypes") List<PermissionActionType> actionTypes,
        Pageable pageable
    );

    /**
     * Find all audit logs with pagination (for admin audit review)
     */
    Page<PermissionAuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);

    /**
     * Delete old audit logs (for data retention policy)
     */
    @Query("DELETE FROM PermissionAuditLog p WHERE p.changedAt < :before")
    int deleteOldAuditLogs(@Param("before") Instant before);

    /**
     * Find audit logs matching search criteria
     */
    @Query("SELECT p FROM PermissionAuditLog p WHERE " +
           "(LOWER(p.targetUsername) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.changedByUsername) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.targetRoleCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.resource) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.changedAt DESC")
    Page<PermissionAuditLog> search(@Param("searchTerm") String searchTerm, Pageable pageable);
}
