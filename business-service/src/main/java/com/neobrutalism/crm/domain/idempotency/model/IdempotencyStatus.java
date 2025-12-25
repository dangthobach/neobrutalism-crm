package com.neobrutalism.crm.domain.idempotency.model;

/**
 * Status of an idempotent operation.
 */
public enum IdempotencyStatus {
    /**
     * Operation is currently in progress.
     */
    IN_PROGRESS,

    /**
     * Operation completed successfully.
     */
    COMPLETED,

    /**
     * Operation failed.
     */
    FAILED
}
