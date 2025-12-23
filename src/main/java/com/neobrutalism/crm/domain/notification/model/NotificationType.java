package com.neobrutalism.crm.domain.notification.model;

/**
 * Types of notifications
 */
public enum NotificationType {
    INFO,                   // General information
    SUCCESS,                // Success message
    WARNING,                // Warning message
    ERROR,                  // Error message
    TASK,                   // Task-related notification
    TASK_ASSIGNED,          // Task assigned notification
    TASK_UPDATED,           // Task updated notification
    TASK_COMPLETED,         // Task completed notification
    TASK_OVERDUE,           // Task overdue notification
    DEADLINE_APPROACHING,   // Deadline approaching notification
    COMMENT,                // Comment notification
    COMMENT_ADDED,          // Comment added notification
    MENTION,                // User mention notification
    SYSTEM,                 // System notification
    EMAIL,                  // Email notification
    SMS,                    // SMS notification
    PUSH                    // Push notification
}
