package com.neobrutalism.crm.domain.user.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when user status changes
 */
@Getter
@Setter
public class UserStatusChangedEvent extends DomainEvent {

    private UserStatus oldStatus;
    private UserStatus newStatus;

    public UserStatusChangedEvent() {
        super();
    }

    public UserStatusChangedEvent(String aggregateId, UserStatus oldStatus, UserStatus newStatus, String occurredBy) {
        super("UserStatusChanged", aggregateId, "User", occurredBy);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", getAggregateId());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("changedBy", getOccurredBy());
        return payload;
    }
}
