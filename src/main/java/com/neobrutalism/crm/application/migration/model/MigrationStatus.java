package com.neobrutalism.crm.application.migration.model;

/**
 * Migration job status
 */
public enum MigrationStatus {
    PENDING,        // Job created, waiting to start
    PROCESSING,     // Currently processing
    COMPLETED,      // Successfully completed
    FAILED,         // Failed with errors
    CANCELLED,      // Manually cancelled
    STUCK;          // Detected as stuck (no heartbeat)
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}

