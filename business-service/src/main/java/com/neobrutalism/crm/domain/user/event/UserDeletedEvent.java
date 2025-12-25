package com.neobrutalism.crm.domain.user.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a user is deleted
 */
@Getter
@Setter
public class UserDeletedEvent extends DomainEvent {

    private String username;

    public UserDeletedEvent() {
        super();
    }

    public UserDeletedEvent(String aggregateId, String username, String occurredBy) {
        super("UserDeleted", aggregateId, "User", occurredBy);
        this.username = username;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", getAggregateId());
        payload.put("username", username);
        payload.put("deletedBy", getOccurredBy());
        return payload;
    }
}
