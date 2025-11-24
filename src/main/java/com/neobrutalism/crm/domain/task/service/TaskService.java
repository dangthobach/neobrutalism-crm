package com.neobrutalism.crm.domain.task.service;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.common.security.UserContext;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.domain.task.dto.TaskRequest;
import com.neobrutalism.crm.domain.task.event.TaskAssignedEvent;
import com.neobrutalism.crm.domain.task.event.TaskCancelledEvent;
import com.neobrutalism.crm.domain.task.event.TaskCompletedEvent;
import com.neobrutalism.crm.domain.task.event.TaskCreatedEvent;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskStatus;
import com.neobrutalism.crm.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing tasks
 * Extends BaseService for standard CRUD operations with lifecycle hooks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService extends BaseService<Task> {

    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;
    private final UserContext userContext;

    @Override
    protected BaseRepository<Task> getRepository() {
        return taskRepository;
    }

    @Override
    protected String getEntityName() {
        return "Task";
    }

    /**
     * Create new task
     */
    @Transactional
    public Task create(TaskRequest request, String createdBy) {
        Task task = new Task();
        mapRequestToEntity(request, task);
        task.setTenantId(TenantContext.getCurrentTenant());

        beforeCreate(task);
        Task saved = taskRepository.save(task);
        afterCreate(saved);

        // Publish domain event
        eventPublisher.publish(new TaskCreatedEvent(
                saved.getId().toString(),
                saved.getTitle(),
                saved.getPriority(),
                saved.getAssignedToId(),
                saved.getDueDate(),
                createdBy
        ));

        return saved;
    }

    /**
     * Update existing task
     */
    @Transactional
    public Task update(UUID id, TaskRequest request, String updatedBy) {
        Task task = findById(id);
        mapRequestToEntity(request, task);

        beforeUpdate(task);
        Task updated = taskRepository.save(task);
        afterUpdate(updated);

        return updated;
    }

    /**
     * Assign task to user
     */
    @Transactional
    public Task assignTo(UUID taskId, UUID userId, String assignedBy) {
        Task task = findById(taskId);
        task.assignTo(userId, assignedBy);

        Task updated = taskRepository.save(task);

        // Publish domain event
        eventPublisher.publish(new TaskAssignedEvent(
                updated.getId().toString(),
                updated.getTitle(),
                userId,
                UUID.fromString(assignedBy)
        ));

        return updated;
    }

    /**
     * Start working on task
     */
    @Transactional
    public Task start(UUID id, String changedBy) {
        Task task = findById(id);
        task.start(changedBy);

        Task updated = taskRepository.save(task);
        return updated;
    }

    /**
     * Submit task for review
     */
    @Transactional
    public Task submitForReview(UUID id, String changedBy) {
        Task task = findById(id);
        task.submitForReview(changedBy);

        Task updated = taskRepository.save(task);
        return updated;
    }

    /**
     * Complete task
     */
    @Transactional
    public Task complete(UUID id, String changedBy) {
        Task task = findById(id);
        task.complete(changedBy);

        Task updated = taskRepository.save(task);

        // Publish domain event
        eventPublisher.publish(new TaskCompletedEvent(
                updated.getId().toString(),
                updated.getTitle(),
                updated.getCompletedAt(),
                changedBy
        ));

        return updated;
    }

    /**
     * Cancel task with reason
     */
    @Transactional
    public Task cancel(UUID id, String reason, String changedBy) {
        Task task = findById(id);
        task.cancel(changedBy, reason);

        Task updated = taskRepository.save(task);

        // Publish domain event
        eventPublisher.publish(new TaskCancelledEvent(
                updated.getId().toString(),
                updated.getTitle(),
                reason,
                changedBy
        ));

        return updated;
    }

    /**
     * Update task progress percentage
     */
    @Transactional
    public Task updateProgress(UUID id, int percentage, String updatedBy) {
        Task task = findById(id);
        task.updateProgress(percentage);

        beforeUpdate(task);
        Task updated = taskRepository.save(task);
        afterUpdate(updated);

        return updated;
    }

    /**
     * Get tasks assigned to user
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksAssignedTo(UUID userId) {
        return taskRepository.findByAssignedToIdAndDeletedFalse(userId);
    }

    /**
     * Get tasks assigned by user
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksAssignedBy(UUID userId) {
        return taskRepository.findByAssignedByIdAndDeletedFalse(userId);
    }

    /**
     * Get tasks by status
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatusAndDeletedFalse(status);
    }

    /**
     * Get tasks for user by status
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByUserAndStatus(UUID userId, TaskStatus status) {
        return taskRepository.findByAssignedToIdAndStatusAndDeletedFalse(userId, status);
    }

    /**
     * Get overdue tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdueTasks(Instant.now());
    }

    /**
     * Get upcoming tasks for user
     */
    @Transactional(readOnly = true)
    public List<Task> getUpcomingTasks(UUID userId, Instant startDate, Instant endDate) {
        return taskRepository.findUpcomingTasks(userId, startDate, endDate);
    }

    /**
     * Get related tasks by entity type and ID
     */
    @Transactional(readOnly = true)
    public List<Task> getRelatedTasks(String relatedToType, UUID relatedToId) {
        return taskRepository.findByRelatedToTypeAndRelatedToIdAndDeletedFalse(relatedToType, relatedToId);
    }

    /**
     * Count tasks by user and status
     */
    @Transactional(readOnly = true)
    public Long countTasksByUserAndStatus(UUID userId, TaskStatus status) {
        return taskRepository.countByAssignedToIdAndStatusAndDeletedFalse(userId, status);
    }

    /**
     * Get tasks by organization
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByOrganization(UUID organizationId) {
        return taskRepository.findByOrganizationIdAndDeletedFalse(organizationId);
    }

    /**
     * Get tasks by branch
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByBranch(UUID branchId) {
        return taskRepository.findByBranchIdAndDeletedFalse(branchId);
    }

    /**
     * Helper method to map request DTO to entity
     * Automatically sets organizationId from current user context
     */
    private void mapRequestToEntity(TaskRequest request, Task task) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setAssignedToId(request.getAssignedToId());
        task.setRelatedToType(request.getRelatedToType());
        task.setRelatedToId(request.getRelatedToId());
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setChecklist(request.getChecklist());

        // Auto-set organizationId from authenticated user context (ignore request value)
        String orgId = userContext.getCurrentOrganizationId().orElse(null);
        if (orgId != null) {
            task.setOrganizationId(UUID.fromString(orgId));
            log.debug("Setting task organizationId from user context: {}", orgId);
        }

        // BranchId can be set from request or context
        if (request.getBranchId() != null) {
            task.setBranchId(request.getBranchId());
        }
    }

}
