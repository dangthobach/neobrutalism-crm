package com.neobrutalism.crm.domain.permission.service;

import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.service.NotificationService;
import com.neobrutalism.crm.domain.permission.model.PermissionAuditLog;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled Service for Permission Audit Maintenance and Monitoring
 *
 * Performs periodic tasks:
 * - Cleanup old audit logs (data retention)
 * - Monitor for suspicious activity
 * - Generate security alerts
 */
@Service
@ConditionalOnProperty(name = "audit.scheduled.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PermissionAuditScheduledService {

    private final PermissionAuditService auditService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Cleanup old audit logs monthly
     * Runs on the 1st day of each month at 2 AM
     * Default retention: 365 days (1 year)
     */
    @Scheduled(cron = "${audit.cleanup.cron:0 0 2 1 * ?}")
    public void cleanupOldAuditLogs() {
        log.info("Starting monthly audit log cleanup");

        try {
            int retentionDays = 365; // Default: 1 year
            int deletedCount = auditService.cleanupOldAuditLogs(retentionDays);

            log.info("Audit log cleanup completed: deleted {} logs older than {} days",
                    deletedCount, retentionDays);

            // Notify admins about cleanup
            if (deletedCount > 0) {
                notifyAdmins(
                    "Audit Log Cleanup Complete",
                    String.format("Deleted %d audit logs older than %d days", deletedCount, retentionDays)
                );
            }
        } catch (Exception e) {
            log.error("Failed to cleanup old audit logs", e);
            notifyAdmins("Audit Log Cleanup Failed", "Error: " + e.getMessage());
        }
    }

    /**
     * Monitor for suspicious activity every hour
     * Runs every hour at minute 0
     */
    @Scheduled(cron = "${audit.monitoring.cron:0 0 * * * ?}")
    public void monitorSuspiciousActivity() {
        log.debug("Running hourly suspicious activity monitoring");

        try {
            // Get failed attempts in last hour
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            Page<PermissionAuditLog> failedAttempts = auditService.getFailedAttempts(
                PageRequest.of(0, 100)
            );

            int suspiciousUserCount = 0;

            // Check each unique user for suspicious activity
            for (PermissionAuditLog log : failedAttempts.getContent()) {
                if (log.getTargetUserId() != null) {
                    boolean isSuspicious = auditService.hasSuspiciousActivity(log.getTargetUserId());

                    if (isSuspicious) {
                        suspiciousUserCount++;
                        handleSuspiciousUser(log.getTargetUserId(), log.getTargetUsername());
                    }
                }
            }

            if (suspiciousUserCount > 0) {
                log.warn("Detected suspicious activity for {} users", suspiciousUserCount);
            }
        } catch (Exception e) {
            log.error("Failed to monitor suspicious activity", e);
        }
    }

    /**
     * Monitor critical security events daily
     * Runs every day at 9 AM
     */
    @Scheduled(cron = "${audit.critical.monitoring.cron:0 0 9 * * ?}")
    public void monitorCriticalEvents() {
        log.info("Running daily critical security events monitoring");

        try {
            // Get critical events from last 24 hours
            Page<PermissionAuditLog> criticalEvents = auditService.getCriticalEvents(
                PageRequest.of(0, 50)
            );

            if (criticalEvents.getTotalElements() > 0) {
                log.warn("Found {} critical security events in last 24 hours",
                        criticalEvents.getTotalElements());

                // Notify security team
                notifySecurityTeam(
                    "Critical Security Events Detected",
                    String.format("Found %d critical security events in last 24 hours. " +
                                 "Please review the audit logs.",
                                 criticalEvents.getTotalElements())
                );

                // Log details
                for (PermissionAuditLog event : criticalEvents.getContent()) {
                    log.warn("Critical event: type={}, user={}, target={}, time={}",
                            event.getActionType(),
                            event.getChangedByUsername(),
                            event.getTargetUsername(),
                            event.getChangedAt());
                }
            }
        } catch (Exception e) {
            log.error("Failed to monitor critical events", e);
        }
    }

    /**
     * Generate weekly audit statistics report
     * Runs every Monday at 8 AM
     */
    @Scheduled(cron = "${audit.weekly.report.cron:0 0 8 * * MON}")
    public void generateWeeklyReport() {
        log.info("Generating weekly audit statistics report");

        try {
            Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant now = Instant.now();

            List<Object[]> statistics = auditService.getAuditStatistics(weekAgo, now);

            StringBuilder report = new StringBuilder();
            report.append("=== Weekly Audit Report ===\n");
            report.append(String.format("Period: %s to %s\n\n", weekAgo, now));

            long totalEvents = 0;
            for (Object[] stat : statistics) {
                String actionType = stat[0].toString();
                Long count = (Long) stat[1];
                totalEvents += count;
                report.append(String.format("%-35s: %d\n", actionType, count));
            }

            report.append(String.format("\nTotal Events: %d\n", totalEvents));

            log.info("Weekly audit report:\n{}", report);

            // Notify admins
            notifyAdmins("Weekly Audit Statistics Report", report.toString());
        } catch (Exception e) {
            log.error("Failed to generate weekly audit report", e);
        }
    }

    /**
     * Handle suspicious user activity
     */
    private void handleSuspiciousUser(UUID userId, String username) {
        log.warn("Suspicious activity detected for user: {} ({})", username, userId);

        try {
            // Get recent failed attempts
            long failedAttempts = auditService.getAuditLogsForUser(
                userId,
                PageRequest.of(0, 1)
            ).getTotalElements();

            // Notify security team
            notifySecurityTeam(
                "Suspicious Activity Detected",
                String.format("User %s (%s) has %d failed permission attempts in last 24 hours",
                             username, userId, failedAttempts)
            );

            // Optionally: Lock account or trigger additional security measures
            // userService.flagForReview(userId);
        } catch (Exception e) {
            log.error("Failed to handle suspicious user: {}", userId, e);
        }
    }

    /**
     * Notify administrators
     */
    private void notifyAdmins(String subject, String message) {
        try {
            // Get all admin users
            List<UUID> adminUserIds = userRepository.findAdminUserIds();

            for (UUID adminId : adminUserIds) {
                notificationService.createNotification(
                    adminId,
                    subject,
                    message,
                    NotificationType.SYSTEM,
                    1, // priority
                    null, // actionUrl
                    "AUDIT", // entityType
                    null // entityId
                );
            }

            log.debug("Notified {} admins: {}", adminUserIds.size(), subject);
        } catch (Exception e) {
            log.error("Failed to notify admins", e);
        }
    }

    /**
     * Notify security team (ADMIN + SECURITY_OFFICER roles)
     */
    private void notifySecurityTeam(String subject, String message) {
        try {
            // Get security team user IDs
            List<UUID> securityUserIds = userRepository.findSecurityTeamUserIds();

            for (UUID userId : securityUserIds) {
                notificationService.createNotification(
                    userId,
                    subject,
                    message,
                    NotificationType.WARNING,
                    2, // higher priority for security
                    "/admin/permissions/audit", // actionUrl
                    "SECURITY_ALERT", // entityType
                    null // entityId
                );
            }

            log.debug("Notified {} security team members: {}", securityUserIds.size(), subject);
        } catch (Exception e) {
            log.error("Failed to notify security team", e);
        }
    }
}
