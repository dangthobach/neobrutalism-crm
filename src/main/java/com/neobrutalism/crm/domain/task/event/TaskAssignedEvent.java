package com.neobrutalism.crm.domain.task.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a task is assigned to a user
 */
@Getter
@Setter
public class TaskAssignedEvent extends DomainEvent {

    private String title;
    private UUID assignedToId;
    private UUID assignedById;

    public TaskAssignedEvent(String aggregateId, String title, UUID assignedToId, UUID assignedById) {
        super("TaskAssigned", aggregateId, "Task", assignedById.toString());
        this.title = title;
        this.assignedToId = assignedToId;
        this.assignedById = assignedById;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", getAggregateId());
        payload.put("title", title);
        payload.put("assignedToId", assignedToId);
        payload.put("assignedById", assignedById);
        return payload;
    }
}
