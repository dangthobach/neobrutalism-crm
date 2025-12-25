package com.neobrutalism.crm.common.event;

import com.neobrutalism.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Outbox Event entity for Transactional Outbox Pattern
 * Ensures reliable domain event delivery even in case of failures
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_published", columnList = "published, occurred_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_outbox_event_type", columnList = "event_type"),
        @Index(name = "idx_outbox_retry", columnList = "published, retry_count, next_retry_at")
})
public class OutboxEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "occurred_by", length = 100)
    private String occurredBy;

    @Builder.Default
    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Builder.Default
    @Column(name = "max_retries")
    private Integer maxRetries = 5;

    /**
     * Create OutboxEvent from DomainEvent
     */
    public static OutboxEvent from(DomainEvent event, String payload) {
        return OutboxEvent.builder()
                .eventId(event.getEventId())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(payload)
                .occurredAt(event.getOccurredAt())
                .occurredBy(event.getOccurredBy())
                .published(false)
                .retryCount(0)
                .maxRetries(5)
                .build();
    }

    /**
     * Mark event as published
     */
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
        this.nextRetryAt = null;
        this.lastError = null;
    }

    /**
     * Record a failed publish attempt
     */
    public void recordFailure(String error) {
        this.retryCount++;
        this.lastError = error;

        // Exponential backoff: 1min, 2min, 4min, 8min, 16min
        long backoffMinutes = (long) Math.pow(2, retryCount - 1);
        this.nextRetryAt = Instant.now().plusSeconds(backoffMinutes * 60);
    }

    /**
     * Check if event should be retried
     */
    public boolean shouldRetry() {
        if (published) {
            return false;
        }

        if (retryCount >= maxRetries) {
            return false;
        }

        if (nextRetryAt != null && Instant.now().isBefore(nextRetryAt)) {
            return false;
        }

        return true;
    }

    /**
     * Check if event has exceeded max retries
     */
    public boolean hasExceededMaxRetries() {
        return retryCount >= maxRetries;
    }
}
