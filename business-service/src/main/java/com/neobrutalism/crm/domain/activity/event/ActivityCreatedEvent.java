package com.neobrutalism.crm.domain.activity.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when an activity is created
 */
@Getter
@Setter
public class ActivityCreatedEvent extends DomainEvent {

    private String subject;
    private ActivityType activityType;
    private UUID ownerId;
    private Instant scheduledStartAt;

    public ActivityCreatedEvent() {
        super();
    }

    public ActivityCreatedEvent(String aggregateId, String subject, ActivityType activityType,
                                UUID ownerId, Instant scheduledStartAt, String occurredBy) {
        super("ActivityCreated", aggregateId, "Activity", occurredBy);
        this.subject = subject;
        this.activityType = activityType;
        this.ownerId = ownerId;
        this.scheduledStartAt = scheduledStartAt;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", getAggregateId());
        payload.put("subject", subject);
        payload.put("activityType", activityType);
        payload.put("ownerId", ownerId);
        payload.put("scheduledStartAt", scheduledStartAt);
        payload.put("createdBy", getOccurredBy());
        return payload;
    }
}
