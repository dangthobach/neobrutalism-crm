package com.neobrutalism.crm.domain.task.service;

import com.neobrutalism.crm.common.security.UserContext;
import com.neobrutalism.crm.domain.task.dto.TaskActivityResponse;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskActivity;
import com.neobrutalism.crm.domain.task.model.TaskStatus;
import com.neobrutalism.crm.domain.task.repository.TaskActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing task activity timeline
 * Logs all events related to tasks for audit and activity feed
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskActivityService {

    private final TaskActivityRepository taskActivityRepository;
    private final UserContext userContext;

    /**
     * Log task creation
     */
    @Transactional
    public void logTaskCreated(Task task, String username) {
        TaskActivity activity = TaskActivity.create(
                task.getId(),
                "CREATED",
                "Task created: " + task.getTitle(),
                getUserId(),
                username,
                task.getOrganizationId()
        ).withMetadata("priority", task.getPriority().toString())
         .withMetadata("status", task.getStatus().toString());

        if (task.getAssignedToId() != null) {
            activity.withMetadata("assignedToId", task.getAssignedToId().toString());
        }

        taskActivityRepository.save(activity);
        log.debug("Logged task creation: {}", task.getId());
    }

    /**
     * Log task status change
     */
    @Transactional
    public void logStatusChanged(UUID taskId, TaskStatus oldStatus, TaskStatus newStatus, 
                                 String username, UUID organizationId) {
        TaskActivity activity = TaskActivity.create(
                taskId,
                "STATUS_CHANGED",
                "Status changed from " + oldStatus + " to " + newStatus,
                getUserId(),
                username,
                organizationId
        ).withMetadata("oldStatus", oldStatus.toString())
         .withMetadata("newStatus", newStatus.toString());

        taskActivityRepository.save(activity);
        log.debug("Logged status change for task {}: {} -> {}", taskId, oldStatus, newStatus);
    }

    /**
     * Log task assignment
     */
    @Transactional
    public void logTaskAssigned(UUID taskId, UUID assigneeId, String assigneeName, 
                               String username, UUID organizationId) {
        TaskActivity activity = TaskActivity.create(
                taskId,
                "ASSIGNED",
                "Assigned to " + assigneeName,
                getUserId(),
                username,
                organizationId
        ).withMetadata("assignedToId", assigneeId.toString())
         .withMetadata("assignedToName", assigneeName);

        taskActivityRepository.save(activity);
        log.debug("Logged task assignment: {} -> {}", taskId, assigneeId);
    }

    /**
     * Log comment added
     */
    @Transactional
    public void logCommentAdded(UUID taskId, UUID commentId, String username, UUID organizationId) {
        TaskActivity activity = TaskActivity.create(
                taskId,
                "COMMENT_ADDED",
                "Added a comment",
                getUserId(),
                username,
                organizationId
        ).withMetadata("commentId", commentId.toString());

        taskActivityRepository.save(activity);
        log.debug("Logged comment added for task {}", taskId);
    }

    /**
     * Log checklist item completed
     */
    @Transactional
    public void logChecklistProgress(UUID taskId, int completed, int total, 
                                     String username, UUID organizationId) {
        TaskActivity activity = TaskActivity.create(
                taskId,
                "CHECKLIST_UPDATED",
                String.format("Checklist progress: %d/%d items completed", completed, total),
                getUserId(),
                username,
                organizationId
        ).withMetadata("completed", completed)
         .withMetadata("total", total)
         .withMetadata("percentage", total > 0 ? (completed * 100 / total) : 0);

        taskActivityRepository.save(activity);
        log.debug("Logged checklist progress for task {}: {}/{}", taskId, completed, total);
    }

    /**
     * Log task updated
     */
    @Transactional
    public void logTaskUpdated(UUID taskId, String field, Object oldValue, Object newValue, 
                              String username, UUID organizationId) {
        TaskActivity activity = TaskActivity.create(
                taskId,
                "UPDATED",
                "Updated " + field,
                getUserId(),
                username,
                organizationId
        ).withMetadata("field", field)
         .withMetadata("oldValue", oldValue != null ? oldValue.toString() : null)
         .withMetadata("newValue", newValue != null ? newValue.toString() : null);

        taskActivityRepository.save(activity);
        log.debug("Logged task update: {} - {}", taskId, field);
    }

    /**
     * Log task deleted
     */
    @Transactional
    public void logTaskDeleted(UUID taskId, String username, UUID organizationId) {
        TaskActivity activity = TaskActivity.create(
                taskId,
                "DELETED",
                "Task deleted",
                getUserId(),
                username,
                organizationId
        );

        taskActivityRepository.save(activity);
        log.debug("Logged task deletion: {}", taskId);
    }

    /**
     * Get all activities for a task
     */
    @Transactional(readOnly = true)
    public List<TaskActivityResponse> getTaskActivities(UUID taskId) {
        return taskActivityRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get activities with pagination
     */
    @Transactional(readOnly = true)
    public Page<TaskActivityResponse> getTaskActivities(UUID taskId, Pageable pageable) {
        return taskActivityRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get activities by type
     */
    @Transactional(readOnly = true)
    public List<TaskActivityResponse> getActivitiesByType(UUID taskId, String activityType) {
        return taskActivityRepository.findByTaskIdAndActivityTypeOrderByCreatedAtDesc(taskId, activityType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Count activities for a task
     */
    @Transactional(readOnly = true)
    public long countActivities(UUID taskId) {
        return taskActivityRepository.countByTaskId(taskId);
    }

    /**
     * Convert entity to response DTO
     */
    private TaskActivityResponse toResponse(TaskActivity activity) {
        return TaskActivityResponse.builder()
                .id(activity.getId())
                .taskId(activity.getTaskId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .userId(activity.getUserId())
                .username(activity.getUsername())
                .metadata(activity.getMetadata())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    /**
     * Get current user ID from context
     */
    private UUID getUserId() {
        return userContext.getCurrentUserId()
                .map(UUID::fromString)
                .orElse(null);
    }
}
