package com.neobrutalism.crm.domain.user.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a user is updated
 */
@Getter
@Setter
public class UserUpdatedEvent extends DomainEvent {

    private String username;

    public UserUpdatedEvent() {
        super();
    }

    public UserUpdatedEvent(String aggregateId, String username, String occurredBy) {
        super("UserUpdated", aggregateId, "User", occurredBy);
        this.username = username;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", getAggregateId());
        payload.put("username", username);
        payload.put("updatedBy", getOccurredBy());
        return payload;
    }
}
