package com.neobrutalism.crm.domain.group.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class GroupCreatedEvent extends DomainEvent {
    private String name;
    private String code;

    public GroupCreatedEvent() {
        super();
    }

    public GroupCreatedEvent(String aggregateId, String name, String code, String occurredBy) {
        super("GroupCreated", aggregateId, "Group", occurredBy);
        this.name = name;
        this.code = code;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", getAggregateId());
        payload.put("name", name);
        payload.put("code", code);
        payload.put("createdBy", getOccurredBy());
        return payload;
    }
}
