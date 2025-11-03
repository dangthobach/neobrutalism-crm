package com.neobrutalism.crm.domain.task.model;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Task entity - represents tasks/to-dos in the CRM system
 * Extends TenantAwareAggregateRoot for multi-tenancy and domain event support
 */
@Entity
@Table(name = "tasks",
        indexes = {
                @Index(name = "idx_task_assigned_to", columnList = "assigned_to_id"),
                @Index(name = "idx_task_assigned_by", columnList = "assigned_by_id"),
                @Index(name = "idx_task_tenant", columnList = "tenant_id"),
                @Index(name = "idx_task_related", columnList = "related_to_type, related_to_id"),
                @Index(name = "idx_task_due_date", columnList = "due_date"),
                @Index(name = "idx_task_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task extends TenantAwareAggregateRoot<TaskStatus> {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 50)
    private TaskPriority priority = TaskPriority.MEDIUM;

    // Assignment
    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "assigned_by_id")
    private UUID assignedById;

    // Related entity (polymorphic relationship)
    @Column(name = "related_to_type", length = 50)
    private String relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY, ACTIVITY

    @Column(name = "related_to_id")
    private UUID relatedToId;

    // Scheduling
    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "actual_hours")
    private Integer actualHours;

    // Progress
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    // Checklist stored as JSON text
    @Column(name = "checklist", columnDefinition = "TEXT")
    private String checklist;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "branch_id")
    private UUID branchId;

    /**
     * Get initial status for new tasks
     */
    @Override
    protected TaskStatus getInitialStatus() {
        return TaskStatus.TODO;
    }

    /**
     * Define allowed state transitions
     */
    @Override
    protected Set<TaskStatus> getAllowedTransitions(TaskStatus currentStatus) {
        return switch (currentStatus) {
            case TODO -> Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED);
            case IN_PROGRESS -> Set.of(TaskStatus.IN_REVIEW, TaskStatus.DONE, TaskStatus.CANCELLED, TaskStatus.TODO);
            case IN_REVIEW -> Set.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS);
            case DONE, CANCELLED -> Set.of(); // Terminal states
        };
    }

    /**
     * Hook method called when status changes
     */
    @Override
    protected void onStatusChanged(TaskStatus oldStatus, TaskStatus newStatus) {
        if (newStatus == TaskStatus.DONE && completedAt == null) {
            completedAt = Instant.now();
            progressPercentage = 100;
        }
    }

    // Business methods

    /**
     * Assign task to user
     */
    public void assignTo(UUID userId, String assignedBy) {
        this.assignedToId = userId;
        this.assignedById = UUID.fromString(assignedBy); // Assuming assignedBy is user ID string
    }

    /**
     * Start working on task
     */
    public void start(String changedBy) {
        transitionTo(TaskStatus.IN_PROGRESS, changedBy, "Task started");
    }

    /**
     * Submit task for review
     */
    public void submitForReview(String changedBy) {
        transitionTo(TaskStatus.IN_REVIEW, changedBy, "Task submitted for review");
    }

    /**
     * Complete task
     */
    public void complete(String changedBy) {
        transitionTo(TaskStatus.DONE, changedBy, "Task completed");
    }

    /**
     * Cancel task
     */
    public void cancel(String changedBy, String reason) {
        transitionTo(TaskStatus.CANCELLED, changedBy, reason);
    }

    /**
     * Update progress
     */
    public void updateProgress(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
        }
        this.progressPercentage = percentage;

        // Auto complete if 100%
        if (percentage == 100 && getStatus() != TaskStatus.DONE) {
            this.completedAt = Instant.now();
        }
    }

    /**
     * Check if task is overdue
     */
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        return Instant.now().isAfter(dueDate) &&
               (getStatus() == TaskStatus.TODO || getStatus() == TaskStatus.IN_PROGRESS);
    }

    /**
     * Check if task is completed
     */
    public boolean isCompleted() {
        return getStatus() == TaskStatus.DONE;
    }
}
