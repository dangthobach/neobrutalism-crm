package com.neobrutalism.crm.domain.notification.model;

/**
 * Status of notification delivery
 */
public enum NotificationStatus {
    PENDING,        // Notification created but not sent
    SENT,           // Notification sent successfully
    DELIVERED,      // Notification delivered to recipient
    READ,           // Notification read by recipient
    FAILED,         // Notification sending failed
    CANCELLED       // Notification cancelled
}
