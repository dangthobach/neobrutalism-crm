package com.neobrutalism.crm.domain.activity.model;

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
 * Activity entity - represents CRM activities (calls, meetings, tasks, etc.)
 * Extends TenantAwareAggregateRoot for multi-tenancy and domain event support
 */
@Entity
@Table(name = "activities",
        indexes = {
                @Index(name = "idx_activity_owner", columnList = "owner_id"),
                @Index(name = "idx_activity_tenant", columnList = "tenant_id"),
                @Index(name = "idx_activity_related", columnList = "related_to_type, related_to_id"),
                @Index(name = "idx_activity_scheduled", columnList = "scheduled_start_at"),
                @Index(name = "idx_activity_status", columnList = "status"),
                @Index(name = "idx_activity_type", columnList = "activity_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Activity extends TenantAwareAggregateRoot<ActivityStatus> {

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 50)
    private ActivityPriority priority = ActivityPriority.MEDIUM;

    // Ownership
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Related entity (polymorphic relationship)
    @Column(name = "related_to_type", length = 50)
    private String relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY

    @Column(name = "related_to_id")
    private UUID relatedToId;

    // Scheduling
    @Column(name = "scheduled_start_at")
    private Instant scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private Instant scheduledEndAt;

    @Column(name = "actual_start_at")
    private Instant actualStartAt;

    @Column(name = "actual_end_at")
    private Instant actualEndAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "location", length = 255)
    private String location;

    // Outcome
    @Column(name = "outcome", columnDefinition = "TEXT")
    private String outcome;

    @Column(name = "next_steps", columnDefinition = "TEXT")
    private String nextSteps;

    // Recurrence
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern", length = 100)
    private String recurrencePattern;  // DAILY, WEEKLY, MONTHLY

    @Column(name = "parent_activity_id")
    private UUID parentActivityId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "branch_id")
    private UUID branchId;

    /**
     * Get initial status for new activities
     */
    @Override
    protected ActivityStatus getInitialStatus() {
        return ActivityStatus.PLANNED;
    }

    /**
     * Define allowed state transitions
     */
    @Override
    protected Set<ActivityStatus> getAllowedTransitions(ActivityStatus currentStatus) {
        return switch (currentStatus) {
            case PLANNED -> Set.of(ActivityStatus.IN_PROGRESS, ActivityStatus.CANCELLED, ActivityStatus.RESCHEDULED);
            case IN_PROGRESS -> Set.of(ActivityStatus.COMPLETED, ActivityStatus.CANCELLED);
            case RESCHEDULED -> Set.of(ActivityStatus.PLANNED, ActivityStatus.CANCELLED);
            case COMPLETED, CANCELLED -> Set.of(); // Terminal states
        };
    }

    /**
     * Hook method called when status changes
     */
    @Override
    protected void onStatusChanged(ActivityStatus oldStatus, ActivityStatus newStatus) {
        if (newStatus == ActivityStatus.IN_PROGRESS && actualStartAt == null) {
            actualStartAt = Instant.now();
        }
        if (newStatus == ActivityStatus.COMPLETED && actualEndAt == null) {
            actualEndAt = Instant.now();
        }
    }

    // Business methods

    /**
     * Start the activity
     */
    public void start(String changedBy) {
        transitionTo(ActivityStatus.IN_PROGRESS, changedBy, "Activity started");
    }

    /**
     * Complete the activity
     */
    public void complete(String changedBy, String outcome) {
        this.outcome = outcome;
        transitionTo(ActivityStatus.COMPLETED, changedBy, "Activity completed");
    }

    /**
     * Cancel the activity
     */
    public void cancel(String changedBy, String reason) {
        transitionTo(ActivityStatus.CANCELLED, changedBy, reason);
    }

    /**
     * Reschedule the activity
     */
    public void reschedule(Instant newStartAt, Instant newEndAt, String changedBy, String reason) {
        this.scheduledStartAt = newStartAt;
        this.scheduledEndAt = newEndAt;
        transitionTo(ActivityStatus.RESCHEDULED, changedBy, reason);
    }

    /**
     * Check if activity is overdue
     */
    public boolean isOverdue() {
        if (scheduledEndAt == null) {
            return false;
        }
        return Instant.now().isAfter(scheduledEndAt) &&
               (getStatus() == ActivityStatus.PLANNED || getStatus() == ActivityStatus.IN_PROGRESS);
    }

    /**
     * Check if activity is in the past
     */
    public boolean isPast() {
        if (scheduledStartAt == null) {
            return false;
        }
        return Instant.now().isAfter(scheduledStartAt);
    }

    /**
     * Calculate actual duration in minutes
     */
    public Integer getActualDurationMinutes() {
        if (actualStartAt == null || actualEndAt == null) {
            return null;
        }
        return (int) java.time.Duration.between(actualStartAt, actualEndAt).toMinutes();
    }
}
