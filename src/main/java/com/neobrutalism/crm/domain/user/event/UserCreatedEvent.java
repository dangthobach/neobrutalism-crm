package com.neobrutalism.crm.domain.user.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a user is created
 */
@Getter
@Setter
public class UserCreatedEvent extends DomainEvent {

    private String username;
    private String email;

    public UserCreatedEvent() {
        super();
    }

    public UserCreatedEvent(String aggregateId, String username, String email, String occurredBy) {
        super("UserCreated", aggregateId, "User", occurredBy);
        this.username = username;
        this.email = email;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", getAggregateId());
        payload.put("username", username);
        payload.put("email", email);
        payload.put("createdBy", getOccurredBy());
        return payload;
    }
}
