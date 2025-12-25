package com.neobrutalism.crm.domain.organization.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain event fired when an organization is created
 */
@Getter
@Setter
public class OrganizationCreatedEvent extends DomainEvent {

    private String name;
    private String code;

    public OrganizationCreatedEvent() {
        super();
    }

    public OrganizationCreatedEvent(String organizationId, String name, String code, String createdBy) {
        super("OrganizationCreated", organizationId, "Organization", createdBy);
        this.name = name;
        this.code = code;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", getAggregateId());
        payload.put("name", name);
        payload.put("code", code);
        payload.put("createdBy", getOccurredBy());
        return payload;
    }
}
