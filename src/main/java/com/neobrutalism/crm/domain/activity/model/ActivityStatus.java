package com.neobrutalism.crm.domain.activity.model;

/**
 * Activity status for state machine
 */
public enum ActivityStatus {
    PLANNED,        // Activity is planned/scheduled
    IN_PROGRESS,    // Activity is in progress
    COMPLETED,      // Activity is completed
    CANCELLED,      // Activity is cancelled
    RESCHEDULED     // Activity is rescheduled
}
