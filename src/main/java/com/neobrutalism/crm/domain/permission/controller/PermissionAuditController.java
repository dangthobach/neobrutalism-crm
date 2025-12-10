package com.neobrutalism.crm.domain.permission.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.permission.model.PermissionActionType;
import com.neobrutalism.crm.domain.permission.model.PermissionAuditLog;
import com.neobrutalism.crm.domain.permission.service.PermissionAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Permission Audit Logs
 *
 * Provides endpoints for querying and analyzing permission changes
 */
@Slf4j
@RestController
@RequestMapping("/api/permission-audit")
@RequiredArgsConstructor
public class PermissionAuditController {

    private final PermissionAuditService auditService;

    /**
     * Get all audit logs (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getAllAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", auditLogs));
    }

    /**
     * Get audit logs for a specific user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getAuditLogsForUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getAuditLogsForUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("User audit logs retrieved", auditLogs));
    }

    /**
     * Get audit logs by action type
     */
    @GetMapping("/action-type/{actionType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getAuditLogsByActionType(
            @PathVariable PermissionActionType actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getAuditLogsByActionType(actionType, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs by action type retrieved", auditLogs));
    }

    /**
     * Get audit logs within date range
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getAuditLogsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs by date range retrieved", auditLogs));
    }

    /**
     * Get critical security events
     */
    @GetMapping("/critical-events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getCriticalEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getCriticalEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Critical security events retrieved", auditLogs));
    }

    /**
     * Get failed permission attempts
     */
    @GetMapping("/failed-attempts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getFailedAttempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getFailedAttempts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Failed permission attempts retrieved", auditLogs));
    }

    /**
     * Search audit logs
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> searchAuditLogs(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.searchAuditLogs(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", auditLogs));
    }

    /**
     * Get audit statistics for a time period
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object[]>>> getAuditStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        // Default to last 30 days if not provided
        if (startDate == null) {
            startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        List<Object[]> statistics = auditService.getAuditStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Audit statistics retrieved", statistics));
    }

    /**
     * Get recent activity for current user
     */
    @GetMapping("/my-activity")
    public ResponseEntity<ApiResponse<Page<PermissionAuditLog>>> getMyActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Get current user ID from security context
        // For now, using placeholder - should get from authentication principal
        UUID currentUserId = UUID.randomUUID(); // TODO: Get from SecurityContextHolder

        Pageable pageable = PageRequest.of(page, size);
        Page<PermissionAuditLog> auditLogs = auditService.getRecentActivity(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved", auditLogs));
    }

    /**
     * Check for suspicious activity for a user
     */
    @GetMapping("/suspicious-activity/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<Boolean>> checkSuspiciousActivity(@PathVariable UUID userId) {
        boolean hasSuspiciousActivity = auditService.hasSuspiciousActivity(userId);
        String message = hasSuspiciousActivity
                ? "Suspicious activity detected for user"
                : "No suspicious activity detected";
        return ResponseEntity.ok(ApiResponse.success(message, hasSuspiciousActivity));
    }

    /**
     * Get audit logs by session (for correlating related changes)
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionAuditLog>>> getAuditLogsBySession(
            @PathVariable String sessionId
    ) {
        List<PermissionAuditLog> auditLogs = auditService.getAuditLogsBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session audit logs retrieved", auditLogs));
    }

    /**
     * Clean up old audit logs (admin maintenance)
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldAuditLogs(
            @RequestParam(defaultValue = "365") int retentionDays
    ) {
        int deletedCount = auditService.cleanupOldAuditLogs(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Deleted %d audit logs older than %d days", deletedCount, retentionDays),
                deletedCount
        ));
    }
}
