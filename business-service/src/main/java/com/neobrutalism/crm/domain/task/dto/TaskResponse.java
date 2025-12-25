package com.neobrutalism.crm.domain.task.dto;

import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskPriority;
import com.neobrutalism.crm.domain.task.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;

    // Assignment
    private UUID assignedToId;
    private UUID assignedById;

    // Related entity
    private String relatedToType;
    private UUID relatedToId;

    // Scheduling
    private Instant dueDate;
    private Instant completedAt;
    private Integer estimatedHours;
    private Integer actualHours;

    // Progress
    private Integer progressPercentage;

    // Checklist
    private String checklist;

    // Organization context
    private UUID organizationId;
    private UUID branchId;

    // Status tracking
    private Instant statusChangedAt;
    private String statusChangedBy;
    private String statusReason;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    // Tenant
    private String tenantId;

    // Version
    private Long version;

    // Computed fields
    private Boolean isOverdue;
    private Boolean isCompleted;

    /**
     * Convert entity to response DTO
     */
    public static TaskResponse fromEntity(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedToId(task.getAssignedToId())
                .assignedById(task.getAssignedById())
                .relatedToType(task.getRelatedToType())
                .relatedToId(task.getRelatedToId())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .progressPercentage(task.getProgressPercentage())
                .checklist(task.getChecklist())
                .organizationId(task.getOrganizationId())
                .branchId(task.getBranchId())
                .statusChangedAt(task.getStatusChangedAt())
                .statusChangedBy(task.getStatusChangedBy())
                .statusReason(task.getStatusReason())
                .createdAt(task.getCreatedAt())
                .createdBy(task.getCreatedBy())
                .updatedAt(task.getUpdatedAt())
                .updatedBy(task.getUpdatedBy())
                .tenantId(task.getTenantId())
                .version(task.getVersion())
                .isOverdue(task.isOverdue())
                .isCompleted(task.isCompleted())
                .build();
    }
}
