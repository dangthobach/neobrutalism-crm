package com.neobrutalism.crm.domain.role.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RoleUpdatedEvent extends DomainEvent {
    private String name;

    public RoleUpdatedEvent() {
        super();
    }

    public RoleUpdatedEvent(String aggregateId, String name, String occurredBy) {
        super("RoleUpdated", aggregateId, "Role", occurredBy);
        this.name = name;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roleId", getAggregateId());
        payload.put("name", name);
        payload.put("updatedBy", getOccurredBy());
        return payload;
    }
}
