package com.neobrutalism.crm.domain.notification.model;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entity representing user notification preferences
 * Supports multi-channel notifications (in-app, email, SMS)
 * Optimized for 1M users, 50K CCU with proper indexing
 */
@Entity
@Table(
    name = "notification_preferences",
    indexes = {
        @Index(name = "idx_notif_pref_user_org", columnList = "user_id,organization_id"),
        @Index(name = "idx_notif_pref_user_type", columnList = "user_id,notification_type"),
        @Index(name = "idx_notif_pref_org", columnList = "organization_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_type_org", columnNames = {"user_id", "notification_type", "organization_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends AuditableEntity {

    /**
     * User who owns this preference
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Organization context for multi-tenancy
     */
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * Type of notification this preference applies to
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    /**
     * Enable/disable in-app notifications
     */
    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;

    /**
     * Enable/disable email notifications
     */
    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    /**
     * Enable/disable SMS notifications
     */
    @Builder.Default
    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    /**
     * Optional: Quiet hours start time (format: HH:mm)
     * Notifications will be queued during quiet hours
     */
    @Column(name = "quiet_hours_start", length = 5)
    private String quietHoursStart;

    /**
     * Get created date (alias for createdAt)
     */
    public java.time.Instant getCreatedDate() {
        return getCreatedAt();
    }
    
    /**
     * Get last modified date (alias for updatedAt)
     */
    public java.time.Instant getLastModifiedDate() {
        return getUpdatedAt();
    }
    
    /**
     * Optional: Quiet hours end time (format: HH:mm)
     */
    @Column(name = "quiet_hours_end", length = 5)
    private String quietHoursEnd;

    /**
     * Enable digest mode (daily summary instead of individual notifications)
     */
    @Builder.Default
    @Column(name = "digest_mode_enabled", nullable = false)
    private Boolean digestModeEnabled = false;

    /**
     * Digest delivery time (format: HH:mm)
     */
    @Column(name = "digest_time", length = 5)
    private String digestTime;

    /**
     * Check if any channel is enabled
     */
    public boolean isAnyChannelEnabled() {
        return Boolean.TRUE.equals(inAppEnabled) ||
               Boolean.TRUE.equals(emailEnabled) ||
               Boolean.TRUE.equals(smsEnabled);
    }

    /**
     * Check if notification should be sent based on preferences
     */
    public boolean shouldSendNotification() {
        return isAnyChannelEnabled() && !Boolean.TRUE.equals(digestModeEnabled);
    }
}
