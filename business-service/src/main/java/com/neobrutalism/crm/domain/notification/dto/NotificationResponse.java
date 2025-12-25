package com.neobrutalism.crm.domain.notification.dto;

import com.neobrutalism.crm.domain.notification.model.Notification;
import com.neobrutalism.crm.domain.notification.model.NotificationStatus;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private NotificationStatus status;
    private UUID recipientId;
    private UUID senderId;
    private String entityType;
    private UUID entityId;
    private String actionUrl;
    private Boolean isRead;
    private Instant readAt;
    private Instant sentAt;
    private Integer priority;
    private Boolean emailSent;
    private Instant createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .status(notification.getStatus())
                .recipientId(notification.getRecipientId())
                .senderId(notification.getSenderId())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .actionUrl(notification.getActionUrl())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .sentAt(notification.getSentAt())
                .priority(notification.getPriority())
                .emailSent(notification.getEmailSent())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
