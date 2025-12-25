package com.neobrutalism.crm.domain.notification.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for queued notifications (e.g., during quiet hours)
 * Notifications are stored here and sent later by scheduled job
 */
@Entity
@Table(
    name = "notification_queue",
    indexes = {
        @Index(name = "idx_notif_queue_scheduled", columnList = "scheduled_at,status"),
        @Index(name = "idx_notif_queue_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notif_queue_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationQueue extends TenantAwareEntity {

    /**
     * Recipient user ID
     */
    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    /**
     * Notification title
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Notification message body
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * Notification type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50)
    private NotificationType notificationType;

    /**
     * Priority (0 = low, 1 = normal, 2 = high)
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * Action URL
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /**
     * Entity type (e.g., "TASK", "MESSAGE")
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Entity ID
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * When this notification should be sent
     */
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    /**
     * Queue status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private QueueStatus status = QueueStatus.QUEUED;

    /**
     * Number of send attempts
     */
    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Error message if sending failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * When notification was actually sent
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Queue status enum
     */
    public enum QueueStatus {
        QUEUED,      // Waiting to be sent
        SENDING,     // Currently being processed
        SENT,        // Successfully sent
        FAILED       // Failed to send after retries
    }

    /**
     * Mark as sending
     */
    public void markAsSending() {
        this.status = QueueStatus.SENDING;
        this.attemptCount++;
    }

    /**
     * Mark as sent
     */
    public void markAsSent() {
        this.status = QueueStatus.SENT;
        this.sentAt = Instant.now();
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = QueueStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Check if can retry sending
     */
    public boolean canRetry() {
        return this.attemptCount < 3 && this.status == QueueStatus.FAILED;
    }
}
