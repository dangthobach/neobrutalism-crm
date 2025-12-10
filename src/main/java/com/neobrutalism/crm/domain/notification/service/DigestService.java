package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.common.email.EmailService;
import com.neobrutalism.crm.domain.notification.dto.NotificationDigest;
import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import com.neobrutalism.crm.domain.notification.model.NotificationStatus;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.repository.NotificationRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing notification digests
 * Sends daily email summaries of notifications for users with digest mode enabled
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigestService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final EmailService emailService;

    /**
     * Send daily digests (runs every hour and checks if any user's digest time has arrived)
     * This is more flexible than a fixed time for all users
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void processDailyDigests() {
        log.info("Processing daily notification digests");

        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();

        // Find all users with digest mode enabled and digest time = current hour
        List<User> users = userRepository.findAll();
        int digestsSent = 0;

        for (User user : users) {
            if (user.getDeleted()) continue;

            try {
                // Get user's notification preferences
                List<NotificationPreference> preferences = notificationPreferenceService
                        .getUserPreferences(user.getId(), user.getOrganizationId());

                // Check if any preference has digest mode enabled with matching time
                boolean shouldSendDigest = preferences.stream()
                        .anyMatch(pref -> Boolean.TRUE.equals(pref.getDigestModeEnabled())
                                       && pref.getDigestTime() != null
                                       && isDigestTime(pref.getDigestTime(), currentHour));

                if (shouldSendDigest) {
                    sendDigestToUser(user);
                    digestsSent++;
                }
            } catch (Exception e) {
                log.error("Failed to process digest for user {}", user.getId(), e);
            }
        }

        log.info("Finished processing daily digests. Sent {} digests", digestsSent);
    }

    /**
     * Check if current hour matches digest time
     */
    private boolean isDigestTime(String digestTime, int currentHour) {
        try {
            LocalTime time = LocalTime.parse(digestTime);
            return time.getHour() == currentHour;
        } catch (Exception e) {
            log.warn("Invalid digest time format: {}", digestTime);
            return false;
        }
    }

    /**
     * Send digest email to a user
     */
    @Transactional
    protected void sendDigestToUser(User user) {
        log.info("Sending digest to user: {} ({})", user.getFullName(), user.getEmail());

        // Get all unread/pending notifications from last 24 hours
        Instant yesterday = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Notification> notifications = notificationRepository.findRecentNotifications(user.getId(), yesterday);

        // Filter to only PENDING notifications (not yet sent due to digest mode)
        List<Notification> pendingNotifications = notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.PENDING)
                .collect(Collectors.toList());

        if (pendingNotifications.isEmpty()) {
            log.info("No pending notifications for user {}, skipping digest", user.getId());
            return;
        }

        // Group notifications by type
        NotificationDigest digest = buildDigest(pendingNotifications);

        // Send email
        try {
            String subject = String.format("Your Daily Notification Digest - %d new notifications",
                    pendingNotifications.size());
            String htmlBody = buildDigestEmailHtml(user, digest, pendingNotifications);

            emailService.sendHtmlEmail(
                    user.getEmail(),
                    subject,
                    htmlBody
            );

            // Mark all notifications as delivered
            for (Notification notification : pendingNotifications) {
                notification.markAsDelivered();
                notification.setEmailSent(true);
                notification.setEmailSentAt(Instant.now());
            }
            notificationRepository.saveAll(pendingNotifications);

            log.info("Digest sent successfully to user {}: {} notifications", user.getId(), pendingNotifications.size());

        } catch (Exception e) {
            log.error("Failed to send digest email to user {}", user.getId(), e);
        }
    }

    /**
     * Build digest summary from notifications
     */
    private NotificationDigest buildDigest(List<Notification> notifications) {
        Map<NotificationType, Long> countByType = notifications.stream()
                .collect(Collectors.groupingBy(
                        Notification::getNotificationType,
                        Collectors.counting()
                ));

        Map<String, Long> countByEntity = notifications.stream()
                .filter(n -> n.getEntityType() != null)
                .collect(Collectors.groupingBy(
                        Notification::getEntityType,
                        Collectors.counting()
                ));

        long highPriorityCount = notifications.stream()
                .filter(Notification::isHighPriority)
                .count();

        return NotificationDigest.builder()
                .totalCount(notifications.size())
                .highPriorityCount((int) highPriorityCount)
                .countByType(countByType)
                .countByEntity(countByEntity)
                .notifications(notifications)
                .build();
    }

    /**
     * Build HTML email body for digest
     */
    private String buildDigestEmailHtml(User user, NotificationDigest digest, List<Notification> notifications) {
        StringBuilder html = new StringBuilder();

        // Email header
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5; }");
        html.append(".container { max-width: 600px; margin: 20px auto; background: white; border: 4px solid #000; box-shadow: 8px 8px 0 rgba(0,0,0,1); }");
        html.append(".header { background: #8b5cf6; color: white; padding: 30px; border-bottom: 4px solid #000; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 900; }");
        html.append(".summary { padding: 20px; background: #f3f4f6; border-bottom: 4px solid #000; }");
        html.append(".summary-item { display: inline-block; margin: 10px 15px 10px 0; padding: 10px 15px; background: white; border: 2px solid #000; font-weight: bold; }");
        html.append(".notification { padding: 20px; border-bottom: 2px solid #e5e7eb; }");
        html.append(".notification:last-child { border-bottom: none; }");
        html.append(".notification-title { font-weight: bold; font-size: 16px; margin-bottom: 5px; color: #000; }");
        html.append(".notification-message { color: #666; margin-bottom: 10px; }");
        html.append(".notification-meta { font-size: 12px; color: #999; }");
        html.append(".badge { display: inline-block; padding: 4px 12px; border: 2px solid #000; font-size: 12px; font-weight: bold; margin-right: 5px; }");
        html.append(".badge-task { background: #dbeafe; }");
        html.append(".badge-mention { background: #fef3c7; }");
        html.append(".badge-system { background: #e5e7eb; }");
        html.append(".badge-high { background: #fecaca; }");
        html.append(".footer { padding: 20px; text-align: center; background: #f9fafb; border-top: 4px solid #000; font-size: 12px; color: #666; }");
        html.append(".button { display: inline-block; padding: 12px 24px; background: #8b5cf6; color: white; text-decoration: none; border: 2px solid #000; font-weight: bold; margin-top: 10px; box-shadow: 4px 4px 0 rgba(0,0,0,1); }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        html.append("<div class='container'>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>📬 Your Daily Digest</h1>");
        html.append("<p style='margin: 5px 0 0 0; opacity: 0.9;'>Hello ").append(user.getFullName()).append("!</p>");
        html.append("</div>");

        // Summary section
        html.append("<div class='summary'>");
        html.append("<h2 style='margin: 0 0 15px 0; font-size: 18px;'>📊 Summary</h2>");
        html.append("<div class='summary-item'>").append(digest.getTotalCount()).append(" Total</div>");
        if (digest.getHighPriorityCount() > 0) {
            html.append("<div class='summary-item' style='background: #fecaca;'>")
                .append(digest.getHighPriorityCount()).append(" High Priority</div>");
        }

        // Count by type
        digest.getCountByType().forEach((type, count) -> {
            String emoji = getTypeEmoji(type);
            html.append("<div class='summary-item'>").append(emoji).append(" ").append(count).append(" ").append(type).append("</div>");
        });
        html.append("</div>");

        // Notifications list
        html.append("<div style='padding: 20px;'>");
        html.append("<h2 style='margin: 0 0 15px 0; font-size: 18px;'>📋 Your Notifications</h2>");

        for (Notification notification : notifications) {
            html.append("<div class='notification'>");

            // Badges
            html.append("<div style='margin-bottom: 10px;'>");
            html.append(getBadgeHtml(notification));
            html.append("</div>");

            // Title
            html.append("<div class='notification-title'>").append(escapeHtml(notification.getTitle())).append("</div>");

            // Message
            if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
                html.append("<div class='notification-message'>").append(escapeHtml(notification.getMessage())).append("</div>");
            }

            // Action link
            if (notification.getActionUrl() != null && !notification.getActionUrl().isEmpty()) {
                html.append("<a href='").append(notification.getActionUrl()).append("' class='button'>View Details →</a>");
            }

            // Meta
            html.append("<div class='notification-meta'>");
            html.append(formatTimestamp(notification.getCreatedAt()));
            html.append("</div>");

            html.append("</div>");
        }

        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>You're receiving this digest because you have digest mode enabled.</p>");
        html.append("<p><a href='#' style='color: #8b5cf6;'>Manage notification preferences</a></p>");
        html.append("<p style='margin-top: 15px; color: #999;'>🤖 Generated with Neobrutalism CRM</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Get badge HTML for notification
     */
    private String getBadgeHtml(Notification notification) {
        StringBuilder badges = new StringBuilder();

        // Type badge
        String typeClass = "badge badge-" + notification.getNotificationType().toString().toLowerCase();
        badges.append("<span class='").append(typeClass).append("'>")
              .append(notification.getNotificationType()).append("</span>");

        // High priority badge
        if (notification.isHighPriority()) {
            badges.append("<span class='badge badge-high'>HIGH PRIORITY</span>");
        }

        return badges.toString();
    }

    /**
     * Get emoji for notification type
     */
    private String getTypeEmoji(NotificationType type) {
        switch (type) {
            case TASK:
            case TASK_ASSIGNED:
            case TASK_UPDATED:
            case TASK_COMPLETED:
            case TASK_OVERDUE:
                return "📋";
            case MENTION: return "💬";
            case COMMENT:
            case COMMENT_ADDED:
                return "💬";
            case SYSTEM: return "⚙️";
            case DEADLINE_APPROACHING: return "⏰";
            case EMAIL: return "✉️";
            case SMS: return "📱";
            case PUSH: return "🔔";
            case INFO: return "ℹ️";
            case SUCCESS: return "✅";
            case WARNING: return "⚠️";
            case ERROR: return "❌";
            default: return "🔔";
        }
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(Instant instant) {
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();

        if (date.equals(today)) {
            return "Today at " + instant.atZone(ZoneId.systemDefault()).toLocalTime().toString();
        } else if (date.equals(today.minusDays(1))) {
            return "Yesterday at " + instant.atZone(ZoneId.systemDefault()).toLocalTime().toString();
        } else {
            return date.toString();
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Send digest immediately (for testing or manual triggering)
     */
    @Transactional
    public void sendDigestNow(UUID userId) {
        log.info("Manually triggering digest for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        sendDigestToUser(user);
    }
}
