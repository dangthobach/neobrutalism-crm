package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.common.websocket.WebSocketService;
import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationPreference;
import com.neobrutalism.crm.domain.notification.model.NotificationQueue;
import com.neobrutalism.crm.domain.notification.model.NotificationStatus;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.repository.NotificationRepository;
import com.neobrutalism.crm.domain.notification.repository.NotificationQueueRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailNotificationService emailNotificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketService webSocketService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final QuietHoursService quietHoursService;
    private final NotificationQueueRepository notificationQueueRepository;

    /**
     * Create and send notification (with quiet hours support)
     */
    @Transactional
    public Notification createNotification(
            UUID recipientId,
            String title,
            String message,
            NotificationType type,
            Integer priority,
            String actionUrl,
            String entityType,
            UUID entityId
    ) {
        log.info("Creating notification for user: {} - {}", recipientId, title);

        // Get user's organization ID (from authenticated context or user lookup)
        UUID organizationId = getOrganizationIdForUser(recipientId);

        // Check user preferences for quiet hours and digest mode
        NotificationPreference preference = notificationPreferenceService.getPreference(recipientId, organizationId, type);

        // If digest mode is enabled, don't send individual notifications
        if (preference != null && Boolean.TRUE.equals(preference.getDigestModeEnabled())) {
            log.info("Digest mode enabled for user {}, notification will be included in daily digest", recipientId);
            // Still create notification record for digest
            Notification notification = new Notification();
            notification.setRecipientId(recipientId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(type);
            notification.setPriority(priority != null ? priority : 0);
            notification.setActionUrl(actionUrl);
            notification.setEntityType(entityType);
            notification.setEntityId(entityId);
            notification.setStatus(NotificationStatus.PENDING);
            return notificationRepository.save(notification);
        }

        // Check if within quiet hours
        if (preference != null && quietHoursService.isWithinQuietHours(preference)) {
            log.info("User {} is in quiet hours, queueing notification for later", recipientId);
            return queueNotification(recipientId, title, message, type, priority, actionUrl, entityType, entityId, preference);
        }

        // Send notification immediately
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setPriority(priority != null ? priority : 0);
        notification.setActionUrl(actionUrl);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setStatus(NotificationStatus.PENDING);

        Notification saved = notificationRepository.save(notification);

        // Send notification asynchronously
        sendNotificationAsync(saved);

        return saved;
    }

    /**
     * Queue notification for later delivery (during quiet hours)
     */
    @Transactional
    protected Notification queueNotification(
            UUID recipientId,
            String title,
            String message,
            NotificationType type,
            Integer priority,
            String actionUrl,
            String entityType,
            UUID entityId,
            NotificationPreference preference
    ) {
        // Calculate when to send (end of quiet hours)
        LocalTime endTime = quietHoursService.calculateQuietHoursEnd(preference);
        Instant scheduledAt;

        if (endTime != null) {
            // Schedule for end of quiet hours
            LocalDate today = LocalDate.now();
            scheduledAt = endTime.atDate(today).atZone(ZoneId.systemDefault()).toInstant();

            // If scheduled time is in the past, schedule for tomorrow
            if (scheduledAt.isBefore(Instant.now())) {
                scheduledAt = endTime.atDate(today.plusDays(1)).atZone(ZoneId.systemDefault()).toInstant();
            }
        } else {
            // Fallback: schedule for 1 hour from now
            scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS);
        }

        NotificationQueue queuedNotification = NotificationQueue.builder()
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .notificationType(type)
                .priority(priority != null ? priority : 0)
                .actionUrl(actionUrl)
                .entityType(entityType)
                .entityId(entityId)
                .scheduledAt(scheduledAt)
                .status(NotificationQueue.QueueStatus.QUEUED)
                .build();

        notificationQueueRepository.save(queuedNotification);

        log.info("Notification queued for user {} to be sent at {}", recipientId, scheduledAt);

        // Create notification record (not sent yet)
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setPriority(priority != null ? priority : 0);
        notification.setActionUrl(actionUrl);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setStatus(NotificationStatus.PENDING);

        return notificationRepository.save(notification);
    }

    /**
     * Process queued notifications (runs every 5 minutes)
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void processQueuedNotifications() {
        log.info("Processing queued notifications");

        List<NotificationQueue> queuedNotifications = notificationQueueRepository
                .findQueuedNotificationsReadyToSend(Instant.now());

        if (queuedNotifications.isEmpty()) {
            log.debug("No queued notifications to process");
            return;
        }

        log.info("Found {} queued notifications to send", queuedNotifications.size());

        for (NotificationQueue queued : queuedNotifications) {
            try {
                queued.markAsSending();
                notificationQueueRepository.save(queued);

                // Create and send the actual notification
                Notification notification = new Notification();
                notification.setRecipientId(queued.getRecipientId());
                notification.setTitle(queued.getTitle());
                notification.setMessage(queued.getMessage());
                notification.setNotificationType(queued.getNotificationType());
                notification.setPriority(queued.getPriority());
                notification.setActionUrl(queued.getActionUrl());
                notification.setEntityType(queued.getEntityType());
                notification.setEntityId(queued.getEntityId());
                notification.setStatus(NotificationStatus.PENDING);

                Notification saved = notificationRepository.save(notification);
                sendNotificationAsync(saved);

                // Mark queue entry as sent
                queued.markAsSent();
                notificationQueueRepository.save(queued);

                log.info("Successfully sent queued notification {} to user {}", queued.getId(), queued.getRecipientId());

            } catch (Exception e) {
                log.error("Failed to send queued notification {}", queued.getId(), e);
                queued.markAsFailed(e.getMessage());
                notificationQueueRepository.save(queued);
            }
        }

        log.info("Finished processing queued notifications");
    }

    /**
     * Cleanup old queued notifications (runs daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldQueuedNotifications() {
        log.info("Cleaning up old queued notifications");

        // Delete sent notifications older than 7 days
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = notificationQueueRepository.deleteOldSentNotifications(sevenDaysAgo);

        log.info("Cleaned up {} old queued notifications", deleted);
    }

    /**
     * Get organization ID for a user
     */
    private UUID getOrganizationIdForUser(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .map(User::getOrganizationId)
                .orElse(null);
    }

    /**
     * Send notification via WebSocket and Email
     */
    @Async
    protected void sendNotificationAsync(Notification notification) {
        try {
            // Send via WebSocket for real-time notification
            sendViaWebSocket(notification);

            // Send via Email if high priority
            if (notification.isHighPriority()) {
                sendViaEmail(notification);
            }

            // Mark as sent
            notification.markAsSent();
            notification.markAsDelivered();
            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getId(), e);
            notification.markAsFailed();
            notificationRepository.save(notification);
        }
    }

    /**
     * Send notification via WebSocket (Enhanced with WebSocketService)
     */
    private void sendViaWebSocket(Notification notification) {
        try {
            webSocketService.sendNotificationToUser(notification.getRecipientId(), notification);
            log.info("Notification sent via WebSocket: {}", notification.getId());
        } catch (Exception e) {
            log.error("Failed to send notification via WebSocket: {}", notification.getId(), e);
        }
    }

    /**
     * Send notification via Email (Enhanced with template support)
     */
    private void sendViaEmail(Notification notification) {
        try {
            // Get recipient email
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(notification.getRecipientId());
            if (userOpt.isEmpty()) {
                log.warn("User not found for notification email: {}", notification.getRecipientId());
                return;
            }

            User user = userOpt.get();
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("User has no email address: {}", user.getId());
                return;
            }

            // Send email using enhanced email notification service with templates
            emailNotificationService.sendNotificationEmail(notification, user);

            notification.setEmailSent(true);
            notification.setEmailSentAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Notification sent via Email: {}", notification.getId());

        } catch (Exception e) {
            log.error("Failed to send notification via Email: {}", notification.getId(), e);
        }
    }

    /**
     * Get notification by ID
     */
    public Notification findById(UUID id) {
        return notificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));
    }

    /**
     * Get all notifications for user
     */
    public List<Notification> findByRecipient(UUID recipientId) {
        return notificationRepository.findByRecipientIdAndDeletedFalseOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get all notifications for user with pagination
     */
    public Page<Notification> findByRecipient(UUID recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientIdAndDeletedFalse(recipientId, pageable);
    }

    /**
     * Get unread notifications
     */
    public List<Notification> findUnreadNotifications(UUID recipientId) {
        return notificationRepository.findUnreadNotifications(recipientId);
    }

    /**
     * Get unread count
     */
    public Long countUnreadNotifications(UUID recipientId) {
        Long count = notificationRepository.countUnreadNotifications(recipientId);
        return count != null ? count : 0L;
    }

    /**
     * Get high priority notifications
     */
    public List<Notification> findHighPriorityNotifications(UUID recipientId) {
        return notificationRepository.findHighPriorityNotifications(recipientId, 1);
    }

    /**
     * Get recent notifications (last 7 days)
     */
    public List<Notification> findRecentNotifications(UUID recipientId) {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return notificationRepository.findRecentNotifications(recipientId, since);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public Notification markAsRead(UUID notificationId) {
        Notification notification = findById(notificationId);
        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        
        // Send WebSocket event for real-time update
        webSocketService.sendNotificationReadEvent(notification.getRecipientId(), notificationId);
        
        // Update unread count
        Long unreadCount = countUnreadNotifications(notification.getRecipientId());
        webSocketService.sendUnreadCountUpdate(notification.getRecipientId(), unreadCount);
        
        return saved;
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public int markAllAsRead(UUID recipientId) {
        int count = notificationRepository.markAllAsRead(recipientId, Instant.now());
        
        // Send unread count update
        webSocketService.sendUnreadCountUpdate(recipientId, 0L);
        
        return count;
    }

    /**
     * Mark notification as unread
     */
    @Transactional
    public Notification markAsUnread(UUID notificationId) {
        Notification notification = findById(notificationId);
        notification.markAsUnread();
        Notification saved = notificationRepository.save(notification);
        
        // Send WebSocket event for real-time update
        Long unreadCount = countUnreadNotifications(notification.getRecipientId());
        webSocketService.sendUnreadCountUpdate(notification.getRecipientId(), unreadCount);
        
        return saved;
    }

    /**
     * Bulk mark notifications as read
     */
    @Transactional
    public int bulkMarkAsRead(List<UUID> notificationIds) {
        log.info("Bulk marking {} notifications as read", notificationIds.size());
        
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
        
        // Update unread counts for affected users
        notifications.stream()
            .map(Notification::getRecipientId)
            .distinct()
            .forEach(recipientId -> {
                Long unreadCount = countUnreadNotifications(recipientId);
                webSocketService.sendUnreadCountUpdate(recipientId, unreadCount);
            });
        
        return notifications.size();
    }

    /**
     * Bulk mark notifications as unread
     */
    @Transactional
    public int bulkMarkAsUnread(List<UUID> notificationIds) {
        log.info("Bulk marking {} notifications as unread", notificationIds.size());
        
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.forEach(Notification::markAsUnread);
        notificationRepository.saveAll(notifications);
        
        // Update unread counts for affected users
        notifications.stream()
            .map(Notification::getRecipientId)
            .distinct()
            .forEach(recipientId -> {
                Long unreadCount = countUnreadNotifications(recipientId);
                webSocketService.sendUnreadCountUpdate(recipientId, unreadCount);
            });
        
        return notifications.size();
    }

    /**
     * Bulk delete notifications
     */
    @Transactional
    public int bulkDelete(List<UUID> notificationIds) {
        log.info("Bulk deleting {} notifications", notificationIds.size());
        
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.forEach(notification -> notification.setDeleted(true));
        notificationRepository.saveAll(notifications);
        
        return notifications.size();
    }

    /**
     * Delete notification (soft delete)
     */
    @Transactional
    public void deleteNotification(UUID id) {
        Notification notification = findById(id);
        notification.setDeleted(true);
        notificationRepository.save(notification);
    }

    /**
     * Delete all notifications for user
     */
    @Transactional
    public int deleteAllByRecipient(UUID recipientId) {
        return notificationRepository.deleteAllByRecipient(recipientId);
    }

    /**
     * Cleanup old notifications (scheduled task)
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications");

        // Delete read notifications older than 30 days
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Notification> oldNotifications = notificationRepository
                .findByStatusAndCreatedBefore(NotificationStatus.READ, thirtyDaysAgo);

        for (Notification notification : oldNotifications) {
            notification.setDeleted(true);
        }

        notificationRepository.saveAll(oldNotifications);
        log.info("Cleaned up {} old notifications", oldNotifications.size());
    }

    /**
     * Send notification to multiple users
     */
    @Transactional
    public void sendBulkNotification(
            List<UUID> recipientIds,
            String title,
            String message,
            NotificationType type,
            Integer priority
    ) {
        log.info("Sending bulk notification to {} users", recipientIds.size());

        for (UUID recipientId : recipientIds) {
            createNotification(recipientId, title, message, type, priority, null, null, null);
        }
    }

    /**
     * Send system notification
     */
    @Transactional
    public Notification sendSystemNotification(UUID recipientId, String title, String message) {
        return createNotification(recipientId, title, message, NotificationType.SYSTEM, 0, null, null, null);
    }

    /**
     * Send task notification
     */
    @Transactional
    public Notification sendTaskNotification(
            UUID recipientId,
            String title,
            String message,
            UUID taskId,
            String actionUrl
    ) {
        return createNotification(recipientId, title, message, NotificationType.TASK, 1, actionUrl, "Task", taskId);
    }

    /**
     * Send mention notification
     */
    @Transactional
    public Notification sendMentionNotification(
            UUID recipientId,
            UUID senderId,
            String message,
            String entityType,
            UUID entityId
    ) {
        Notification notification = createNotification(
                recipientId,
                "You were mentioned",
                message,
                NotificationType.MENTION,
                1,
                null,
                entityType,
                entityId
        );
        notification.setSenderId(senderId);
        return notificationRepository.save(notification);
    }
}
