package com.neobrutalism.crm.domain.organization.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event published when an Organization is updated
 * Used for read model synchronization
 */
@Getter
@Setter
public class OrganizationUpdatedEvent extends DomainEvent {

    private String name;

    public OrganizationUpdatedEvent() {
        super();
    }

    public OrganizationUpdatedEvent(String organizationId, String name, String updatedBy) {
        super("OrganizationUpdated", organizationId, "Organization", updatedBy);
        this.name = name;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", getAggregateId());
        payload.put("name", name);
        payload.put("updatedBy", getOccurredBy());
        return payload;
    }
}
