package com.neobrutalism.crm.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.common.event.EventStore;
import com.neobrutalism.crm.common.event.OutboxEvent;
import com.neobrutalism.crm.common.repository.EventStoreRepository;
import com.neobrutalism.crm.common.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Outbox Event Publisher implementing Transactional Outbox Pattern
 * Ensures reliable event delivery with retry mechanism
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final EventStoreRepository eventStoreRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Store domain event in outbox (within same transaction as entity changes)
     */
    @Transactional
    public void storeInOutbox(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event.getPayload());
            OutboxEvent outboxEvent = OutboxEvent.from(event, payload);
            outboxEventRepository.save(outboxEvent);

            log.debug("Stored event in outbox: {} for aggregate: {}",
                    event.getEventType(), event.getAggregateId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload for outbox", e);
            throw new RuntimeException("Failed to store event in outbox", e);
        }
    }

    /**
     * Store multiple domain events in outbox
     */
    @Transactional
    public void storeAllInOutbox(List<DomainEvent> events) {
        events.forEach(this::storeInOutbox);
    }

    /**
     * Publish pending events from outbox (scheduled job)
     * Runs every 5 seconds
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.interval:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findUnpublishedEvents(Instant.now());

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending events from outbox", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                // Attempt to publish the event
                publishEvent(outboxEvent);

                // Mark as published
                outboxEvent.markAsPublished();
                outboxEventRepository.save(outboxEvent);

                log.info("Successfully published event: {} (attempt {})",
                        outboxEvent.getEventType(), outboxEvent.getRetryCount() + 1);

            } catch (Exception e) {
                // Record failure and schedule retry
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                outboxEvent.recordFailure(errorMessage);
                outboxEventRepository.save(outboxEvent);

                log.error("Failed to publish event: {} (attempt {}/{}). Next retry at: {}",
                        outboxEvent.getEventType(),
                        outboxEvent.getRetryCount(),
                        outboxEvent.getMaxRetries(),
                        outboxEvent.getNextRetryAt(),
                        e);

                // Check if exceeded max retries
                if (outboxEvent.hasExceededMaxRetries()) {
                    log.error("Event {} exceeded max retries and moved to dead letter queue",
                            outboxEvent.getEventId());
                }
            }
        }
    }

    /**
     * Publish a single outbox event
     */
    private void publishEvent(OutboxEvent outboxEvent) {
        try {
            // 1. Persist to EventStore for event sourcing
            EventStore eventStore = EventStore.builder()
                    .eventId(outboxEvent.getEventId())
                    .eventType(outboxEvent.getEventType())
                    .aggregateId(outboxEvent.getAggregateId())
                    .aggregateType(outboxEvent.getAggregateType())
                    .payload(outboxEvent.getPayload())
                    .occurredAt(outboxEvent.getOccurredAt())
                    .occurredBy(outboxEvent.getOccurredBy())
                    .build();
            eventStoreRepository.save(eventStore);

            // 2. Reconstruct DomainEvent for publishing
            DomainEvent domainEvent = reconstructDomainEvent(outboxEvent);

            // 3. Publish to application event bus
            applicationEventPublisher.publishEvent(domainEvent);

            log.debug("Published event to event bus: {}", outboxEvent.getEventType());

        } catch (Exception e) {
            log.error("Failed to publish event: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Event publication failed", e);
        }
    }

    /**
     * Reconstruct DomainEvent from OutboxEvent
     * This is a simplified version - in production you might need event type registry
     */
    private DomainEvent reconstructDomainEvent(OutboxEvent outboxEvent) {
        // Create a generic wrapper event
        return new GenericDomainEvent(
                outboxEvent.getEventType(),
                outboxEvent.getAggregateId(),
                outboxEvent.getAggregateType(),
                outboxEvent.getOccurredBy(),
                outboxEvent.getPayload()
        );
    }

    /**
     * Clean up old published events (scheduled job)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupOldPublishedEvents() {
        // Delete events older than 30 days
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        outboxEventRepository.deleteByPublishedTrueAndPublishedAtBefore(threshold);
        log.info("Cleaned up published outbox events older than {}", threshold);
    }

    /**
     * Get dead letter events (events that exceeded max retries)
     */
    public List<OutboxEvent> getDeadLetterEvents() {
        return outboxEventRepository.findDeadLetterEvents();
    }

    /**
     * Manually retry a dead letter event
     */
    @Transactional
    public void retryDeadLetterEvent(OutboxEvent event) {
        event.setRetryCount(0);
        event.setNextRetryAt(null);
        event.setLastError(null);
        outboxEventRepository.save(event);
        log.info("Reset dead letter event for retry: {}", event.getEventId());
    }

    /**
     * Get outbox statistics
     */
    public OutboxStatistics getStatistics() {
        long pending = outboxEventRepository.countByPublishedFalse();
        long deadLetter = outboxEventRepository.countDeadLetterEvents();
        return new OutboxStatistics(pending - deadLetter, deadLetter);
    }

    /**
     * Generic domain event for outbox publishing
     */
    private static class GenericDomainEvent extends DomainEvent {
        private final String payloadJson;

        public GenericDomainEvent(String eventType, String aggregateId,
                                  String aggregateType, String occurredBy,
                                  String payloadJson) {
            super(eventType, aggregateId, aggregateType, occurredBy);
            this.payloadJson = payloadJson;
        }

        @Override
        public Object getPayload() {
            return payloadJson;
        }
    }

    /**
     * Outbox statistics record
     */
    public record OutboxStatistics(long pendingCount, long deadLetterCount) {
    }
}
