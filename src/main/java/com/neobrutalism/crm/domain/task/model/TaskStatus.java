package com.neobrutalism.crm.domain.task.model;

/**
 * Task status for state machine
 */
public enum TaskStatus {
    TODO,           // Task not started
    IN_PROGRESS,    // Task in progress
    IN_REVIEW,      // Task under review
    DONE,           // Task completed
    CANCELLED       // Task cancelled
}
