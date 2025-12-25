package com.neobrutalism.crm.application.migration.model;

/**
 * Sheet processing status
 */
public enum SheetStatus {
    PENDING,        // Sheet created, waiting to process
    PROCESSING,     // Currently processing
    VALIDATED,      // Validation completed
    COMPLETED,      // Successfully completed
    FAILED,         // Failed with errors
    CANCELLED,      // Manually cancelled
    STUCK;          // Detected as stuck (no heartbeat)
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}

