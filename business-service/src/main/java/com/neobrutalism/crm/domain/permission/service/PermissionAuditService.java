package com.neobrutalism.crm.domain.permission.service;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.domain.permission.model.PermissionActionType;
import com.neobrutalism.crm.domain.permission.model.PermissionAuditLog;
import com.neobrutalism.crm.domain.permission.repository.PermissionAuditLogRepository;
import com.neobrutalism.crm.domain.user.model.DataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing permission audit logs
 *
 * Provides async audit logging to avoid impacting permission operations performance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionAuditService {

    private final PermissionAuditLogRepository auditLogRepository;

    /**
     * Log role assignment (async to avoid blocking)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRoleAssignment(
            UUID changedByUserId,
            String changedByUsername,
            UUID targetUserId,
            String targetUsername,
            String roleCode,
            String reason
    ) {
        try {
            PermissionAuditLog auditLog = PermissionAuditLog.forRoleAssignment(
                changedByUserId,
                changedByUsername,
                targetUserId,
                targetUsername,
                roleCode,
                reason,
                TenantContext.getCurrentTenant()
            );
            auditLogRepository.save(auditLog);
            log.debug("Logged role assignment: user={}, role={}", targetUsername, roleCode);
        } catch (Exception e) {
            log.error("Failed to log role assignment", e);
            // Don't throw - audit logging failure should not break main operation
        }
    }

    /**
     * Log role removal (async)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRoleRemoval(
            UUID changedByUserId,
            String changedByUsername,
            UUID targetUserId,
            String targetUsername,
            String roleCode,
            String reason
    ) {
        try {
            PermissionAuditLog auditLog = PermissionAuditLog.forRoleRemoval(
                changedByUserId,
                changedByUsername,
                targetUserId,
                targetUsername,
                roleCode,
                reason,
                TenantContext.getCurrentTenant()
            );
            auditLogRepository.save(auditLog);
            log.debug("Logged role removal: user={}, role={}", targetUsername, roleCode);
        } catch (Exception e) {
            log.error("Failed to log role removal", e);
        }
    }

    /**
     * Log data scope change (async)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDataScopeChange(
            UUID changedByUserId,
            String changedByUsername,
            UUID targetUserId,
            String targetUsername,
            DataScope oldScope,
            DataScope newScope,
            String reason
    ) {
        try {
            PermissionAuditLog auditLog = PermissionAuditLog.forDataScopeChange(
                changedByUserId,
                changedByUsername,
                targetUserId,
                targetUsername,
                oldScope,
                newScope,
                reason,
                TenantContext.getCurrentTenant()
            );
            auditLogRepository.save(auditLog);
            log.debug("Logged data scope change: user={}, {} -> {}", targetUsername, oldScope, newScope);
        } catch (Exception e) {
            log.error("Failed to log data scope change", e);
        }
    }

    /**
     * Log policy creation (async)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPolicyCreation(
            UUID changedByUserId,
            String changedByUsername,
            String roleCode,
            String resource,
            String action,
            String reason
    ) {
        try {
            PermissionAuditLog auditLog = PermissionAuditLog.forPolicyCreation(
                changedByUserId,
                changedByUsername,
                roleCode,
                resource,
                action,
                reason,
                TenantContext.getCurrentTenant()
            );
            auditLogRepository.save(auditLog);
            log.debug("Logged policy creation: role={}, resource={}, action={}", roleCode, resource, action);
        } catch (Exception e) {
            log.error("Failed to log policy creation", e);
        }
    }

    /**
     * Log policy deletion (async)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPolicyDeletion(
            UUID changedByUserId,
            String changedByUsername,
            String roleCode,
            String resource,
            String action,
            String reason
    ) {
        try {
            PermissionAuditLog auditLog = PermissionAuditLog.forPolicyDeletion(
                changedByUserId,
                changedByUsername,
                roleCode,
                resource,
                action,
                reason,
                TenantContext.getCurrentTenant()
            );
            auditLogRepository.save(auditLog);
            log.debug("Logged policy deletion: role={}, resource={}, action={}", roleCode, resource, action);
        } catch (Exception e) {
            log.error("Failed to log policy deletion", e);
        }
    }

    /**
     * Log custom audit event (async)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditEvent(PermissionAuditLog auditLog) {
        try {
            if (auditLog.getTenantId() == null) {
                auditLog.setTenantId(TenantContext.getCurrentTenant());
            }
            auditLogRepository.save(auditLog);
            log.debug("Logged audit event: type={}", auditLog.getActionType());
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }

    /**
     * Get audit logs for a target user
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getAuditLogsForUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByTargetUserIdOrderByChangedAtDesc(userId, pageable);
    }

    /**
     * Get audit logs by action type
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getAuditLogsByActionType(PermissionActionType actionType, Pageable pageable) {
        return auditLogRepository.findByActionTypeOrderByChangedAtDesc(actionType, pageable);
    }

    /**
     * Get audit logs within date range
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getAuditLogsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Get critical security events
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getCriticalEvents(Pageable pageable) {
        return auditLogRepository.findCriticalEvents(pageable);
    }

    /**
     * Get failed permission attempts
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getFailedAttempts(Pageable pageable) {
        return auditLogRepository.findFailedAttempts(pageable);
    }

    /**
     * Get audit statistics for date range
     */
    @Transactional(readOnly = true)
    public List<Object[]> getAuditStatistics(Instant startDate, Instant endDate) {
        return auditLogRepository.getAuditStatistics(startDate, endDate);
    }

    /**
     * Search audit logs
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> searchAuditLogs(String searchTerm, Pageable pageable) {
        return auditLogRepository.search(searchTerm, pageable);
    }

    /**
     * Get recent activity for a user (last 30 days)
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getRecentActivity(UUID userId, Pageable pageable) {
        return auditLogRepository.findByTargetUserIdOrderByChangedAtDesc(userId, pageable);
    }

    /**
     * Check for suspicious activity (multiple failed attempts)
     */
    @Transactional(readOnly = true)
    public boolean hasSuspiciousActivity(UUID userId) {
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);
        long failedCount = auditLogRepository.countRecentFailedAttempts(userId, last24Hours);
        return failedCount >= 5; // Threshold for suspicious activity
    }

    /**
     * Get all audit logs (admin only)
     */
    @Transactional(readOnly = true)
    public Page<PermissionAuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByChangedAtDesc(pageable);
    }

    /**
     * Get audit logs by session ID (for correlating related changes)
     */
    @Transactional(readOnly = true)
    public List<PermissionAuditLog> getAuditLogsBySession(String sessionId) {
        return auditLogRepository.findBySessionIdOrderByChangedAtAsc(sessionId);
    }

    /**
     * Clean up old audit logs (admin maintenance)
     * Should be called periodically by scheduled job
     */
    @Transactional
    public int cleanupOldAuditLogs(int retentionDays) {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deletedCount = auditLogRepository.deleteOldAuditLogs(cutoffDate);
        log.info("Deleted {} audit logs older than {} days", deletedCount, retentionDays);
        return deletedCount;
    }
}
