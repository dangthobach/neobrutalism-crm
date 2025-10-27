package com.neobrutalism.crm.domain.role.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RoleCreatedEvent extends DomainEvent {
    private String name;
    private String code;

    public RoleCreatedEvent() {
        super();
    }

    public RoleCreatedEvent(String aggregateId, String name, String code, String occurredBy) {
        super("RoleCreated", aggregateId, "Role", occurredBy);
        this.name = name;
        this.code = code;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roleId", getAggregateId());
        payload.put("name", name);
        payload.put("code", code);
        payload.put("createdBy", getOccurredBy());
        return payload;
    }
}
