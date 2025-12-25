package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.event.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OutboxEvent
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find unpublished events ordered by occurred_at
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.published = false
            AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
            AND e.retryCount < e.maxRetries
            ORDER BY e.occurredAt ASC
            """)
    List<OutboxEvent> findUnpublishedEvents(Instant now);

    /**
     * Find events that have exceeded max retries (dead letter queue)
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.published = false
            AND e.retryCount >= e.maxRetries
            ORDER BY e.occurredAt ASC
            """)
    List<OutboxEvent> findDeadLetterEvents();

    /**
     * Find events by aggregate
     */
    List<OutboxEvent> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);

    /**
     * Find events by aggregate type and id
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc(
            String aggregateType, String aggregateId);

    /**
     * Count unpublished events
     */
    long countByPublishedFalse();

    /**
     * Count dead letter events
     */
    @Query("""
            SELECT COUNT(e) FROM OutboxEvent e
            WHERE e.published = false
            AND e.retryCount >= e.maxRetries
            """)
    long countDeadLetterEvents();

    /**
     * Delete old published events (for cleanup)
     */
    void deleteByPublishedTrueAndPublishedAtBefore(Instant threshold);
}
