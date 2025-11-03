package com.neobrutalism.crm.domain.task.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import com.neobrutalism.crm.domain.task.model.TaskPriority;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a task is created
 */
@Getter
@Setter
public class TaskCreatedEvent extends DomainEvent {

    private String title;
    private TaskPriority priority;
    private UUID assignedToId;
    private Instant dueDate;

    public TaskCreatedEvent(String aggregateId, String title, TaskPriority priority,
                           UUID assignedToId, Instant dueDate, String occurredBy) {
        super("TaskCreated", aggregateId, "Task", occurredBy);
        this.title = title;
        this.priority = priority;
        this.assignedToId = assignedToId;
        this.dueDate = dueDate;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", getAggregateId());
        payload.put("title", title);
        payload.put("priority", priority != null ? priority.toString() : null);
        payload.put("assignedToId", assignedToId);
        payload.put("dueDate", dueDate);
        return payload;
    }
}
