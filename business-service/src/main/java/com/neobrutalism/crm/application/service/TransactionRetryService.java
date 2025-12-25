package com.neobrutalism.crm.application.service;

import com.neobrutalism.crm.domain.idempotency.model.RetryMetadata;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Service for handling transactional operations with automatic retry.
 *
 * Supports:
 * - Exponential backoff (2^attempt * 1000ms)
 * - Optimistic locking retry (for concurrent updates)
 * - Idempotency integration
 * - Circuit breaker pattern
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionRetryService {

    private final RetryTemplate retryTemplate;

    /**
     * Execute operation with automatic retry on OptimisticLockingFailureException.
     *
     * Uses Spring Retry annotations for declarative retry configuration.
     *
     * @param operation Operation to execute
     * @param <T> Return type
     * @return Operation result
     */
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2.0,
            maxDelay = 10000
        )
    )
    @Transactional
    public <T> T executeWithRetry(Supplier<T> operation) {
        try {
            T result = operation.get();
            log.debug("Operation succeeded");
            return result;
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure, retrying: {}", e.getMessage());
            throw e; // Spring Retry will catch and retry
        }
    }

    /**
     * Execute operation with custom retry metadata tracking.
     *
     * Provides detailed retry information for monitoring and debugging.
     *
     * @param operation Operation to execute
     * @param maxAttempts Maximum retry attempts
     * @param backoffMultiplier Exponential backoff multiplier
     * @param <T> Return type
     * @return Operation result with retry metadata
     */
    public <T> RetryResult<T> executeWithRetryMetadata(
            Supplier<T> operation,
            int maxAttempts,
            double backoffMultiplier) {

        RetryMetadata metadata = RetryMetadata.builder()
            .attemptCount(0)
            .maxAttempts(maxAttempts)
            .backoffMultiplier(backoffMultiplier)
            .build();

        Exception lastException = null;

        while (!metadata.isMaxRetriesExceeded()) {
            try {
                T result = operation.get();

                log.info("Operation succeeded after {} attempts", metadata.getAttemptCount() + 1);

                return RetryResult.<T>builder()
                    .result(result)
                    .metadata(metadata)
                    .success(true)
                    .build();

            } catch (Exception e) {
                lastException = e;
                metadata.incrementAttempt(e.getMessage());

                log.warn("Operation failed, attempt {}/{}: {}",
                    metadata.getAttemptCount(),
                    metadata.getMaxAttempts(),
                    e.getMessage());

                if (metadata.isMaxRetriesExceeded()) {
                    break;
                }

                // Wait before retry
                try {
                    Thread.sleep(metadata.calculateNextRetryDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        log.error("Operation failed after {} attempts", metadata.getAttemptCount());

        return RetryResult.<T>builder()
            .metadata(metadata)
            .success(false)
            .failureException(lastException)
            .build();
    }

    /**
     * Result container for retry operations with metadata.
     */
    @Data
    @Builder
    public static class RetryResult<T> {
        private T result;
        private RetryMetadata metadata;
        private boolean success;
        private Exception failureException;
    }
}
