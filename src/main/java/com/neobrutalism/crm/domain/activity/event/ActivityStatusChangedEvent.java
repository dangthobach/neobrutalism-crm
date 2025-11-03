package com.neobrutalism.crm.domain.activity.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.activity.model.ActivityStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when activity status changes
 */
@Getter
@Setter
public class ActivityStatusChangedEvent extends DomainEvent {

    private ActivityStatus oldStatus;
    private ActivityStatus newStatus;
    private String reason;

    public ActivityStatusChangedEvent() {
        super();
    }

    public ActivityStatusChangedEvent(String aggregateId, ActivityStatus oldStatus,
                                      ActivityStatus newStatus, String reason, String occurredBy) {
        super("ActivityStatusChanged", aggregateId, "Activity", occurredBy);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", getAggregateId());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("reason", reason);
        payload.put("changedBy", getOccurredBy());
        return payload;
    }
}
