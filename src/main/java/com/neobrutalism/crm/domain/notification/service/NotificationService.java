package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.common.email.EmailService;
import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.common.websocket.WebSocketService;
import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationStatus;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.repository.NotificationRepository;
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

    /**
     * Create and send notification
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
