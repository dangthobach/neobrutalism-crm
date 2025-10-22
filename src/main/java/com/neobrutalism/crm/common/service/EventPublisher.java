package com.neobrutalism.crm.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.common.event.EventStore;
import com.neobrutalism.crm.common.repository.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Event publisher service for publishing and persisting domain events
 * Supports both direct publishing and Transactional Outbox Pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${events.use-outbox:true}")
    private boolean useOutbox;

    /**
     * Publish a single domain event
     * Uses Outbox Pattern by default for reliability
     */
    @Transactional
    public void publish(DomainEvent event) {
        if (useOutbox) {
            // Store in outbox (reliable, survives failures)
            outboxEventPublisher.storeInOutbox(event);
            log.debug("Stored event in outbox: {} for aggregate: {}",
                    event.getEventType(), event.getAggregateId());
        } else {
            // Direct publish (faster but less reliable)
            publishDirectly(event);
        }
    }

    /**
     * Publish multiple domain events
     */
    @Transactional
    public void publishAll(List<DomainEvent> events) {
        if (useOutbox) {
            outboxEventPublisher.storeAllInOutbox(events);
        } else {
            events.forEach(this::publishDirectly);
        }
    }

    /**
     * Direct publish without outbox (for backward compatibility or specific use cases)
     */
    @Transactional
    public void publishDirectly(DomainEvent event) {
        try {
            // Persist event to event store
            String payload = objectMapper.writeValueAsString(event.getPayload());
            EventStore eventStore = EventStore.from(event, payload);
            eventStoreRepository.save(eventStore);

            // Publish event to application event bus
            applicationEventPublisher.publishEvent(event);

            log.debug("Published event directly: {} for aggregate: {}",
                    event.getEventType(), event.getAggregateId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Get events for an aggregate
     */
    public List<EventStore> getEventsForAggregate(String aggregateId) {
        return eventStoreRepository.findByAggregateIdOrderByOccurredAtAsc(aggregateId);
    }

    /**
     * Get events for an aggregate by type
     */
    public List<EventStore> getEventsForAggregate(String aggregateId, String aggregateType) {
        return eventStoreRepository.findByAggregateIdAndAggregateTypeOrderByOccurredAtAsc(
                aggregateId, aggregateType);
    }

    /**
     * Get latest event for an aggregate
     */
    public EventStore getLatestEvent(String aggregateId) {
        return eventStoreRepository.findLatestByAggregateId(aggregateId);
    }

    /**
     * Count events for an aggregate
     */
    public long countEvents(String aggregateId) {
        return eventStoreRepository.countByAggregateId(aggregateId);
    }
}
