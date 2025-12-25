package com.neobrutalism.crm.domain.activity.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when an activity is completed
 */
@Getter
@Setter
public class ActivityCompletedEvent extends DomainEvent {

    private String subject;
    private UUID ownerId;
    private String outcome;

    public ActivityCompletedEvent() {
        super();
    }

    public ActivityCompletedEvent(String aggregateId, String subject, UUID ownerId,
                                  String outcome, String occurredBy) {
        super("ActivityCompleted", aggregateId, "Activity", occurredBy);
        this.subject = subject;
        this.ownerId = ownerId;
        this.outcome = outcome;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", getAggregateId());
        payload.put("subject", subject);
        payload.put("ownerId", ownerId);
        payload.put("outcome", outcome);
        payload.put("completedBy", getOccurredBy());
        return payload;
    }
}
