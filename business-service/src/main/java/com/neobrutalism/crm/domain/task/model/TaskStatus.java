package com.neobrutalism.crm.domain.task.model;

/**
 * Task status for state machine
 */
public enum TaskStatus {
    TODO,           // Task not started
    IN_PROGRESS,    // Task in progress
    IN_REVIEW,      // Task under review
    COMPLETED,      // Task completed (synced with frontend)
    CANCELLED,      // Task cancelled
    ON_HOLD         // Task on hold
}
