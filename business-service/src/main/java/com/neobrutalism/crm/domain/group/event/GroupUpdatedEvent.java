package com.neobrutalism.crm.domain.group.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class GroupUpdatedEvent extends DomainEvent {
    private String name;

    public GroupUpdatedEvent() {
        super();
    }

    public GroupUpdatedEvent(String aggregateId, String name, String occurredBy) {
        super("GroupUpdated", aggregateId, "Group", occurredBy);
        this.name = name;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", getAggregateId());
        payload.put("name", name);
        payload.put("updatedBy", getOccurredBy());
        return payload;
    }
}
