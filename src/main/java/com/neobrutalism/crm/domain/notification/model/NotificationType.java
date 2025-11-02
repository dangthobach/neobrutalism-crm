package com.neobrutalism.crm.domain.notification.model;

/**
 * Types of notifications
 */
public enum NotificationType {
    INFO,           // General information
    SUCCESS,        // Success message
    WARNING,        // Warning message
    ERROR,          // Error message
    TASK,           // Task-related notification
    COMMENT,        // Comment notification
    MENTION,        // User mention notification
    SYSTEM,         // System notification
    EMAIL,          // Email notification
    SMS,            // SMS notification
    PUSH            // Push notification
}
