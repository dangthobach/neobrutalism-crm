package com.neobrutalism.crm.domain.role.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.role.model.RoleStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RoleStatusChangedEvent extends DomainEvent {
    private RoleStatus oldStatus;
    private RoleStatus newStatus;

    public RoleStatusChangedEvent() {
        super();
    }

    public RoleStatusChangedEvent(String aggregateId, RoleStatus oldStatus, RoleStatus newStatus, String occurredBy) {
        super("RoleStatusChanged", aggregateId, "Role", occurredBy);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roleId", getAggregateId());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("changedBy", getOccurredBy());
        return payload;
    }
}
