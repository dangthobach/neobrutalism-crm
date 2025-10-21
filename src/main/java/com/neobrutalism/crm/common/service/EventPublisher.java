package com.neobrutalism.crm.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.common.event.EventStore;
import com.neobrutalism.crm.common.repository.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Event publisher service for publishing and persisting domain events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publish a single domain event
     */
    @Transactional
    public void publish(DomainEvent event) {
        try {
            // Persist event to event store
            String payload = objectMapper.writeValueAsString(event.getPayload());
            EventStore eventStore = EventStore.from(event, payload);
            eventStoreRepository.save(eventStore);

            // Publish event to application event bus
            applicationEventPublisher.publishEvent(event);

            log.debug("Published event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publish multiple domain events
     */
    @Transactional
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
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
