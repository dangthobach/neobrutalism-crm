package com.neobrutalism.crm.domain.activity.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when an activity is cancelled
 */
@Getter
@Setter
public class ActivityCancelledEvent extends DomainEvent {

    private String subject;
    private UUID ownerId;
    private String cancellationReason;

    public ActivityCancelledEvent() {
        super();
    }

    public ActivityCancelledEvent(String aggregateId, String subject, UUID ownerId,
                                  String cancellationReason, String occurredBy) {
        super("ActivityCancelled", aggregateId, "Activity", occurredBy);
        this.subject = subject;
        this.ownerId = ownerId;
        this.cancellationReason = cancellationReason;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", getAggregateId());
        payload.put("subject", subject);
        payload.put("ownerId", ownerId);
        payload.put("cancellationReason", cancellationReason);
        payload.put("cancelledBy", getOccurredBy());
        return payload;
    }
}
