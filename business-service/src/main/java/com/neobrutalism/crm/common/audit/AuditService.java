package com.neobrutalism.crm.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing audit logs
 * Handles async logging and querying of audit events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log audit event asynchronously
     * Uses separate transaction to ensure audit logs are saved even if main transaction fails
     * 
     * @param event Audit event to log
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .tenantId(event.getTenantId())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .userId(event.getUserId())
                .username(event.getUsername())
                .description(event.getDescription())
                .changes(event.getChanges())
                .oldValues(event.getOldValues())
                .newValues(event.getNewValues())
                .requestParams(event.getRequestParams())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .methodName(event.getMethodName())
                .executionTimeMs(event.getExecutionTimeMs())
                .success(event.getSuccess())
                .errorMessage(event.getErrorMessage())
                .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log saved: {} {} on {} (ID: {})", 
                event.getAction(), 
                event.getEntityType(), 
                event.getEntityId(),
                auditLog.getId());

        } catch (Exception e) {
            log.error("Failed to save audit log for {} {} on {}: {}", 
                event.getAction(), 
                event.getEntityType(), 
                event.getEntityId(),
                e.getMessage(), e);
            // Don't throw exception - audit logging should not break main operations
        }
    }

    /**
     * Get audit logs for a specific entity
     * 
     * @param tenantId Tenant ID
     * @param entityType Entity type (e.g., "Customer", "Contract")
     * @param entityId Entity ID
     * @return List of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityAuditHistory(UUID tenantId, String entityType, UUID entityId) {
        return auditLogRepository.findByTenantAndEntity(tenantId, entityType, entityId);
    }

    /**
     * Get audit logs for a tenant with pagination
     * 
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getTenantAuditLogs(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get user activity logs
     * 
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserActivityLogs(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get audit logs by action
     * 
     * @param tenantId Tenant ID
     * @param action Audit action
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(UUID tenantId, AuditAction action, Pageable pageable) {
        return auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable);
    }

    /**
     * Get audit logs by date range
     * 
     * @param tenantId Tenant ID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(
        UUID tenantId, 
        Instant startDate, 
        Instant endDate, 
        Pageable pageable
    ) {
        return auditLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, startDate, endDate, pageable);
    }

    /**
     * Get failed operations
     * 
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return Page of failed audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getFailedOperations(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findFailedOperations(tenantId, pageable);
    }

    /**
     * Get recent activities for dashboard
     * 
     * @param tenantId Tenant ID
     * @param limit Number of activities to retrieve
     * @return List of recent audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentActivities(UUID tenantId, int limit) {
        return auditLogRepository.findRecentActivities(tenantId, Pageable.ofSize(limit)).getContent();
    }

    /**
     * Count audit logs for entity
     * 
     * @param tenantId Tenant ID
     * @param entityType Entity type
     * @param entityId Entity ID
     * @return Count of audit logs
     */
    @Transactional(readOnly = true)
    public long countEntityAuditLogs(UUID tenantId, String entityType, UUID entityId) {
        return auditLogRepository.countByTenantAndEntity(tenantId, entityType, entityId);
    }

    /**
     * Get user activities since date
     * 
     * @param tenantId Tenant ID
     * @param userId User ID
     * @param since Start date
     * @return List of user activities
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getUserActivitiesSince(UUID tenantId, UUID userId, Instant since) {
        return auditLogRepository.findUserActivities(tenantId, userId, since);
    }

    /**
     * Search audit logs by entity type and action
     * 
     * @param tenantId Tenant ID
     * @param entityType Entity type
     * @param action Audit action
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogs(
        UUID tenantId,
        String entityType,
        AuditAction action,
        Pageable pageable
    ) {
        return auditLogRepository.findByTenantEntityAndAction(tenantId, entityType, action, pageable);
    }
}
