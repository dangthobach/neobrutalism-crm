package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Email Notification Service with templates and preference checking
 * Optimized for 1M users with async delivery and queue management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationPreferenceService preferenceService;

    @Value("${notification.email.from:noreply@crm.local}")
    private String fromEmail;

    @Value("${notification.email.from-name:Neobrutalism CRM}")
    private String fromName;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Send notification email with template
     * Checks user preferences before sending
     */
    @Async
    public void sendNotificationEmail(Notification notification, User user) {
        try {
            // Get tenant ID (organization ID)
            String tenantId = notification.getTenantId();
            UUID organizationId = tenantId != null ? UUID.fromString(tenantId) : null;
            
            // Check if email notifications are enabled for this user
            if (!shouldSendEmail(user.getId(), organizationId, notification.getNotificationType())) {
                log.debug("Email notifications disabled for user {} - type {}", 
                    user.getId(), notification.getNotificationType());
                return;
            }

            // Check quiet hours
            if (isInQuietHours(user.getId(), organizationId)) {
                log.debug("User {} is in quiet hours, skipping email", user.getId());
                return;
            }

            // Build email context
            Context context = buildNotificationContext(notification);

            // Process template
            String htmlContent = templateEngine.process("email/notification", context);

            // Send email
            sendHtmlEmail(user.getEmail(), notification.getTitle(), htmlContent);

            log.info("Email notification sent to {} for notification {}", 
                user.getEmail(), notification.getId());

        } catch (Exception e) {
            log.error("Failed to send email notification to {} for notification {}", 
                user.getEmail(), notification.getId(), e);
        }
    }

    /**
     * Send digest email with multiple notifications
     */
    @Async
    public void sendDigestEmail(User user, UUID organizationId, List<Notification> notifications, String digestPeriod) {
        try {
            if (notifications.isEmpty()) {
                return;
            }

            // Build digest context
            Map<String, Object> variables = new HashMap<>();
            variables.put("appName", fromName);
            variables.put("notifications", notifications);
            variables.put("totalNotifications", notifications.size());
            variables.put("digestPeriod", digestPeriod);
            variables.put("notificationsUrl", baseUrl + "/admin/notifications");
            variables.put("preferencesUrl", baseUrl + "/admin/notifications/preferences");

            Context context = new Context();
            context.setVariables(variables);

            // Process template
            String htmlContent = templateEngine.process("email/notification-digest", context);

            // Send email
            String subject = String.format("%s Notification Digest - %s", digestPeriod, fromName);
            sendHtmlEmail(user.getEmail(), subject, htmlContent);

            log.info("Digest email sent to {} with {} notifications", 
                user.getEmail(), notifications.size());

        } catch (Exception e) {
            log.error("Failed to send digest email to {}", user.getEmail(), e);
        }
    }

    /**
     * Check if email should be sent based on user preferences
     */
    private boolean shouldSendEmail(UUID userId, UUID organizationId, NotificationType type) {
        try {
            NotificationPreference preference = preferenceService.getPreference(userId, organizationId, type);
            
            if (preference == null) {
                return true; // Default: send all notifications
            }

            // Check if email channel is enabled
            if (preference.getEmailEnabled() == null || !preference.getEmailEnabled()) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking email preferences for user {}", userId, e);
            return true; // Default to sending on error
        }
    }

    /**
     * Check if current time is in user's quiet hours
     */
    private boolean isInQuietHours(UUID userId, UUID organizationId) {
        try {
            List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId, organizationId);
            
            if (preferences.isEmpty()) {
                return false;
            }

            // Get the first preference with quiet hours set
            NotificationPreference preference = preferences.stream()
                .filter(p -> p.getQuietHoursStart() != null && p.getQuietHoursEnd() != null)
                .findFirst()
                .orElse(null);

            if (preference == null) {
                return false;
            }

            LocalTime now = LocalTime.now(ZoneId.systemDefault());
            LocalTime start = LocalTime.parse(preference.getQuietHoursStart());
            LocalTime end = LocalTime.parse(preference.getQuietHoursEnd());

            // Handle quiet hours that span midnight
            if (start.isBefore(end)) {
                return now.isAfter(start) && now.isBefore(end);
            } else {
                return now.isAfter(start) || now.isBefore(end);
            }

        } catch (Exception e) {
            log.error("Error checking quiet hours for user {}", userId, e);
            return false; // Default to not in quiet hours on error
        }
    }

    /**
     * Build Thymeleaf context for notification email
     */
    private Context buildNotificationContext(Notification notification) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("appName", fromName);
        variables.put("title", notification.getTitle());
        variables.put("message", notification.getMessage());
        variables.put("priority", getPriorityString(notification.getPriority()));
        variables.put("hasAction", notification.getActionUrl() != null);
        variables.put("actionUrl", notification.getActionUrl() != null ? notification.getActionUrl() : "");
        variables.put("entityType", notification.getEntityType());
        variables.put("notificationType", notification.getNotificationType().name());
        variables.put("timestamp", notification.getCreatedAt() != null ? notification.getCreatedAt() : Instant.now());
        variables.put("preferencesUrl", baseUrl + "/admin/notifications/preferences");

        Context context = new Context();
        context.setVariables(variables);
        return context;
    }

    /**
     * Get priority string from priority value
     */
    private String getPriorityString(Integer priority) {
        if (priority == null) return "NORMAL";
        
        return switch (priority) {
            case 0 -> "LOW";
            case 1 -> "NORMAL";
            case 2 -> "HIGH";
            case 3 -> "URGENT";
            default -> "NORMAL";
        };
    }

    /**
     * Send HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Send bulk notification emails
     * Uses batching for better performance
     */
    @Async
    public void sendBulkNotificationEmails(List<User> users, Notification notification) {
        log.info("Sending bulk email notifications to {} users", users.size());
        
        int successCount = 0;
        int failureCount = 0;

        for (User user : users) {
            try {
                sendNotificationEmail(notification, user);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send bulk email to {}", user.getEmail(), e);
                failureCount++;
            }

            // Add small delay to avoid overwhelming mail server
            if (successCount % 100 == 0) {
                try {
                    Thread.sleep(1000); // 1 second delay every 100 emails
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("Bulk email completed: {} success, {} failures", successCount, failureCount);
    }
}
