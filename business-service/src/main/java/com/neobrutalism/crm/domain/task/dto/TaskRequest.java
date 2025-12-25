package com.neobrutalism.crm.domain.task.dto;

import com.neobrutalism.crm.domain.task.model.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for Task creation and update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private TaskPriority priority;

    private UUID assignedToId;

    // Related entity (polymorphic relationship)
    private String relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY, ACTIVITY
    private UUID relatedToId;

    // Scheduling
    private Instant dueDate;

    @Min(value = 0, message = "Estimated hours must be non-negative")
    private Integer estimatedHours;

    @Min(value = 0, message = "Actual hours must be non-negative")
    private Integer actualHours;

    // Progress
    @Min(value = 0, message = "Progress percentage must be between 0 and 100")
    @Max(value = 100, message = "Progress percentage must be between 0 and 100")
    private Integer progressPercentage;

    // Checklist stored as JSON text
    private String checklist;

    // Organization context
    private UUID organizationId;
    private UUID branchId;
}
