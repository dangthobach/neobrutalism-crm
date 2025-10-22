package com.neobrutalism.crm.domain.organization.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event published when an Organization is soft deleted
 * Used for read model synchronization
 */
@Getter
@Setter
public class OrganizationDeletedEvent extends DomainEvent {

    private String name;

    public OrganizationDeletedEvent() {
        super();
    }

    public OrganizationDeletedEvent(String organizationId, String name, String deletedBy) {
        super("OrganizationDeleted", organizationId, "Organization", deletedBy);
        this.name = name;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", getAggregateId());
        payload.put("name", name);
        payload.put("deletedBy", getOccurredBy());
        return payload;
    }
}
