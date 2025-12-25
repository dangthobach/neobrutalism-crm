package com.neobrutalism.crm.common.event;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base domain event class
 */
@Getter
@Setter
public abstract class DomainEvent implements Serializable {

    private String eventId;
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Instant occurredAt;
    private String occurredBy;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }

    protected DomainEvent(String eventType, String aggregateId, String aggregateType, String occurredBy) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredAt = Instant.now();
        this.occurredBy = occurredBy;
    }

    /**
     * Get event payload as JSON-serializable object
     */
    public abstract Object getPayload();
}
