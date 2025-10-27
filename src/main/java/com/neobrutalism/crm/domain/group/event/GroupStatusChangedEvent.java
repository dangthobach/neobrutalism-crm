package com.neobrutalism.crm.domain.group.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.group.model.GroupStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class GroupStatusChangedEvent extends DomainEvent {
    private GroupStatus oldStatus;
    private GroupStatus newStatus;

    public GroupStatusChangedEvent() {
        super();
    }

    public GroupStatusChangedEvent(String aggregateId, GroupStatus oldStatus, GroupStatus newStatus, String occurredBy) {
        super("GroupStatusChanged", aggregateId, "Group", occurredBy);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", getAggregateId());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("changedBy", getOccurredBy());
        return payload;
    }
}
