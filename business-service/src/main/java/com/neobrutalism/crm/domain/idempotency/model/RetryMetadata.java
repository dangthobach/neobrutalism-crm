package com.neobrutalism.crm.domain.idempotency.model;

import lombok.*;
import java.time.Instant;

/**
 * Value Object containing metadata about retry attempts for an operation.
 *
 * Used by Spring Retry to track exponential backoff and circuit breaker state.
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryMetadata {

    /**
     * Number of retry attempts made.
     */
    private int attemptCount;

    /**
     * Maximum retry attempts allowed.
     */
    private int maxAttempts;

    /**
     * Last exception message.
     */
    private String lastException;

    /**
     * Timestamp of last attempt.
     */
    private Instant lastAttemptAt;

    /**
     * Next retry scheduled at.
     */
    private Instant nextRetryAt;

    /**
     * Exponential backoff multiplier.
     */
    private double backoffMultiplier;

    /**
     * Check if max retries exceeded.
     */
    public boolean isMaxRetriesExceeded() {
        return attemptCount >= maxAttempts;
    }

    /**
     * Calculate next retry delay based on exponential backoff.
     */
    public long calculateNextRetryDelay() {
        // 2^attemptCount * 1000ms * backoffMultiplier
        return (long) (Math.pow(2, attemptCount) * 1000 * backoffMultiplier);
    }

    /**
     * Increment attempt count and update timestamps.
     */
    public void incrementAttempt(String exceptionMessage) {
        this.attemptCount++;
        this.lastException = exceptionMessage;
        this.lastAttemptAt = Instant.now();
        this.nextRetryAt = lastAttemptAt.plusMillis(calculateNextRetryDelay());
    }
}
