package com.neobrutalism.crm.common.entity;

import com.neobrutalism.crm.common.event.DomainEvent;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root with Domain Events support for Event Sourcing
 */
@Getter
@MappedSuperclass
public abstract class AggregateRoot<S extends Enum<S>> extends StatefulEntity<S> {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Register a domain event
     */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * Get all domain events (unmodifiable)
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all domain events
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    @Override
    protected void onStatusChanged(S oldStatus, S newStatus) {
        super.onStatusChanged(oldStatus, newStatus);
        // Can register a status change event here if needed
    }
}
