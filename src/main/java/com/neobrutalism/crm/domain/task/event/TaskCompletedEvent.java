package com.neobrutalism.crm.domain.task.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a task is completed
 */
@Getter
@Setter
public class TaskCompletedEvent extends DomainEvent {

    private String title;
    private Instant completedAt;

    public TaskCompletedEvent(String aggregateId, String title, Instant completedAt, String occurredBy) {
        super("TaskCompleted", aggregateId, "Task", occurredBy);
        this.title = title;
        this.completedAt = completedAt;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", getAggregateId());
        payload.put("title", title);
        payload.put("completedAt", completedAt);
        return payload;
    }
}
