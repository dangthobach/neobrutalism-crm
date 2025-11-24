package com.neobrutalism.crm.common.audit.controller;

import com.neobrutalism.crm.common.audit.AuditAction;
import com.neobrutalism.crm.common.audit.AuditLog;
import com.neobrutalism.crm.common.audit.AuditService;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for audit log queries
 * Provides endpoints for viewing audit trail, entity history, and user activity
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Logs", description = "Audit log and activity trail endpoints")
public class AuditLogController {

    private final AuditService auditService;

    /**
     * Get audit logs for current tenant with pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get tenant audit logs", description = "Returns paginated audit logs for the current tenant")
    public ResponseEntity<Page<AuditLog>> getTenantAuditLogs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by action type") @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Filter failed operations only") @RequestParam(defaultValue = "false") boolean failedOnly
    ) {
        UUID tenantIdUUID = UUID.fromString(TenantContext.getCurrentTenant());
        log.debug("Getting audit logs for tenant: {}, page: {}, size: {}", tenantIdUUID, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogs;

        if (failedOnly) {
            auditLogs = auditService.getFailedOperations(tenantIdUUID, pageable);
        } else if (action != null) {
            auditLogs = auditService.getAuditLogsByAction(tenantIdUUID, action, pageable);
        } else if (startDate != null && endDate != null) {
            Instant startInstant = startDate.atZone(ZoneId.systemDefault()).toInstant();
            Instant endInstant = endDate.atZone(ZoneId.systemDefault()).toInstant();
            auditLogs = auditService.getAuditLogsByDateRange(tenantIdUUID, startInstant, endInstant, pageable);
        } else if (userId != null) {
            auditLogs = auditService.getUserActivityLogs(userId, pageable);
        } else {
            auditLogs = auditService.getTenantAuditLogs(tenantIdUUID, pageable);
        }

        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit history for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'USER')")
    @Operation(summary = "Get entity audit history", description = "Returns complete audit trail for a specific entity")
    public ResponseEntity<List<AuditLog>> getEntityHistory(
            @Parameter(description = "Entity type (e.g., 'Customer', 'User')") @PathVariable String entityType,
            @Parameter(description = "Entity ID") @PathVariable UUID entityId
    ) {
        UUID tenantIdUUID = UUID.fromString(TenantContext.getCurrentTenant());
        log.debug("Getting audit history for entity: {} {}", entityType, entityId);
        List<AuditLog> history = auditService.getEntityAuditHistory(tenantIdUUID, entityType, entityId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get recent activities for dashboard
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get recent activities", description = "Returns recent audit activities for dashboard display")
    public ResponseEntity<List<AuditLog>> getRecentActivities(
            @Parameter(description = "Number of activities to return") @RequestParam(defaultValue = "10") int limit
    ) {
        UUID tenantIdUUID = UUID.fromString(TenantContext.getCurrentTenant());
        log.debug("Getting {} recent activities for tenant: {}", limit, tenantIdUUID);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> activities = auditService.getTenantAuditLogs(tenantIdUUID, pageable);

        return ResponseEntity.ok(activities.getContent());
    }

    /**
     * Get user activity logs
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get user activity logs", description = "Returns audit logs for a specific user")
    public ResponseEntity<Page<AuditLog>> getUserActivities(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Getting activity logs for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> activities = auditService.getUserActivityLogs(userId, pageable);
        return ResponseEntity.ok(activities);
    }

    /**
     * Get failed operations for troubleshooting
     */
    @GetMapping("/failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get failed operations", description = "Returns audit logs for failed operations")
    public ResponseEntity<Page<AuditLog>> getFailedOperations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantIdUUID = UUID.fromString(TenantContext.getCurrentTenant());
        log.debug("Getting failed operations for tenant: {}", tenantIdUUID);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> failedOps = auditService.getFailedOperations(tenantIdUUID, pageable);

        return ResponseEntity.ok(failedOps);
    }

    /**
     * Get audit statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get audit statistics", description = "Returns audit statistics for the current tenant")
    public ResponseEntity<AuditStatistics> getAuditStatistics(
            @Parameter(description = "Start date for stats (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date for stats (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        UUID tenantIdUUID = UUID.fromString(TenantContext.getCurrentTenant());
        log.debug("Getting audit statistics for tenant: {}", tenantIdUUID);

        // Calculate statistics
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();
        
        Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();

        Pageable allRecords = Pageable.unpaged();
        Page<AuditLog> allLogs = auditService.getAuditLogsByDateRange(tenantIdUUID, startInstant, endInstant, allRecords);

        long totalOperations = allLogs.getTotalElements();
        long failedOperations = auditService.getFailedOperations(tenantIdUUID, allRecords).getTotalElements();
        long successRate = totalOperations > 0 ? ((totalOperations - failedOperations) * 100 / totalOperations) : 100;

        // Count by action type
        long creates = auditService.getAuditLogsByAction(tenantIdUUID, AuditAction.CREATE, allRecords).getTotalElements();
        long updates = auditService.getAuditLogsByAction(tenantIdUUID, AuditAction.UPDATE, allRecords).getTotalElements();
        long deletes = auditService.getAuditLogsByAction(tenantIdUUID, AuditAction.DELETE, allRecords).getTotalElements();

        AuditStatistics stats = AuditStatistics.builder()
                .tenantId(tenantIdUUID.toString())
                .startDate(start)
                .endDate(end)
                .totalOperations(totalOperations)
                .failedOperations(failedOperations)
                .successRate(successRate)
                .createOperations(creates)
                .updateOperations(updates)
                .deleteOperations(deletes)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * DTO for audit statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class AuditStatistics {
        private String tenantId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private long totalOperations;
        private long failedOperations;
        private long successRate;
        private long createOperations;
        private long updateOperations;
        private long deleteOperations;
    }
}
