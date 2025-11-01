package com.neobrutalism.crm.domain.notification.model;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity
 */
@Getter
@Setter
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notification_type", columnList = "notification_type"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_read", columnList = "is_read"),
        @Index(name = "idx_notification_created", columnList = "created_at"),
        @Index(name = "idx_notification_deleted", columnList = "deleted")
    }
)
public class Notification extends AuditableEntity {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "action_url", length = 1000)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 0=normal, 1=high, 2=urgent

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON metadata

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "push_sent", nullable = false)
    private Boolean pushSent = false;

    @Column(name = "push_sent_at")
    private Instant pushSentAt;

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = Instant.now();
        if (this.status == NotificationStatus.DELIVERED) {
            this.status = NotificationStatus.READ;
        }
    }

    /**
     * Mark notification as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    /**
     * Mark notification as delivered
     */
    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    /**
     * Mark notification as failed
     */
    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    /**
     * Check if notification is high priority
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 1;
    }

    /**
     * Check if notification is urgent
     */
    public boolean isUrgent() {
        return priority != null && priority >= 2;
    }
}
