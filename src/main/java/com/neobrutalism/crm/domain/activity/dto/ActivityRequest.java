package com.neobrutalism.crm.domain.activity.dto;

import com.neobrutalism.crm.domain.activity.model.ActivityPriority;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating/updating Activity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    @Builder.Default
    private ActivityPriority priority = ActivityPriority.MEDIUM;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    // Related entity (polymorphic)
    private String relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY
    private UUID relatedToId;

    // Scheduling
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
    private Integer durationMinutes;
    private String location;

    // Recurrence
    @Builder.Default
    private Boolean isRecurring = false;
    private String recurrencePattern;
    private UUID parentActivityId;

    // Organization context
    private UUID organizationId;
    private UUID branchId;
}
