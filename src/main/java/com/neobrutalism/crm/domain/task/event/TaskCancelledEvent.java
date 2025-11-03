package com.neobrutalism.crm.domain.task.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a task is cancelled
 */
@Getter
@Setter
public class TaskCancelledEvent extends DomainEvent {

    private String title;
    private String reason;

    public TaskCancelledEvent(String aggregateId, String title, String reason, String occurredBy) {
        super("TaskCancelled", aggregateId, "Task", occurredBy);
        this.title = title;
        this.reason = reason;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", getAggregateId());
        payload.put("title", title);
        payload.put("reason", reason);
        return payload;
    }
}
