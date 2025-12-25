package com.neobrutalism.crm.domain.activity.dto;

import com.neobrutalism.crm.domain.activity.model.Activity;
import com.neobrutalism.crm.domain.activity.model.ActivityPriority;
import com.neobrutalism.crm.domain.activity.model.ActivityStatus;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Activity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {

    private UUID id;
    private String subject;
    private String description;
    private ActivityType activityType;
    private ActivityStatus status;
    private ActivityPriority priority;

    // Ownership
    private UUID ownerId;
    private String ownerName;  // Denormalized for display

    // Related entity
    private String relatedToType;
    private UUID relatedToId;
    private String relatedToName;  // Denormalized for display

    // Scheduling
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
    private Instant actualStartAt;
    private Instant actualEndAt;
    private Integer durationMinutes;
    private Integer actualDurationMinutes;
    private String location;

    // Outcome
    private String outcome;
    private String nextSteps;

    // Recurrence
    private Boolean isRecurring;
    private String recurrencePattern;
    private UUID parentActivityId;

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

    // Computed fields
    private Boolean isOverdue;
    private Boolean isPast;

    /**
     * Create response from entity
     */
    public static ActivityResponse fromEntity(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .subject(activity.getSubject())
                .description(activity.getDescription())
                .activityType(activity.getActivityType())
                .status(activity.getStatus())
                .priority(activity.getPriority())
                .ownerId(activity.getOwnerId())
                .relatedToType(activity.getRelatedToType())
                .relatedToId(activity.getRelatedToId())
                .scheduledStartAt(activity.getScheduledStartAt())
                .scheduledEndAt(activity.getScheduledEndAt())
                .actualStartAt(activity.getActualStartAt())
                .actualEndAt(activity.getActualEndAt())
                .durationMinutes(activity.getDurationMinutes())
                .actualDurationMinutes(activity.getActualDurationMinutes())
                .location(activity.getLocation())
                .outcome(activity.getOutcome())
                .nextSteps(activity.getNextSteps())
                .isRecurring(activity.getIsRecurring())
                .recurrencePattern(activity.getRecurrencePattern())
                .parentActivityId(activity.getParentActivityId())
                .organizationId(activity.getOrganizationId())
                .branchId(activity.getBranchId())
                .statusChangedAt(activity.getStatusChangedAt())
                .statusChangedBy(activity.getStatusChangedBy())
                .statusReason(activity.getStatusReason())
                .createdAt(activity.getCreatedAt())
                .createdBy(activity.getCreatedBy())
                .updatedAt(activity.getUpdatedAt())
                .updatedBy(activity.getUpdatedBy())
                .isOverdue(activity.isOverdue())
                .isPast(activity.isPast())
                .build();
    }
}
