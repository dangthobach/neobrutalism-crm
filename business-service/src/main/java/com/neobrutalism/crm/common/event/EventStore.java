package com.neobrutalism.crm.common.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event store entity for persisting domain events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_store", indexes = {
        @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
        @Index(name = "idx_aggregate_type", columnList = "aggregate_type"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_occurred_at", columnList = "occurred_at")
})
public class EventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "occurred_by", length = 100)
    private String occurredBy;

    @Version
    @Column(name = "version")
    private Long version;

    public static EventStore from(DomainEvent event, String payload) {
        return EventStore.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .payload(payload)
                .occurredAt(event.getOccurredAt())
                .occurredBy(event.getOccurredBy())
                .build();
    }
}
