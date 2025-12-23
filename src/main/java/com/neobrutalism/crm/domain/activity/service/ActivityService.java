package com.neobrutalism.crm.domain.activity.service;

import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.activity.dto.ActivityRequest;
import com.neobrutalism.crm.domain.activity.event.ActivityCancelledEvent;
import com.neobrutalism.crm.domain.activity.event.ActivityCompletedEvent;
import com.neobrutalism.crm.domain.activity.event.ActivityCreatedEvent;
import com.neobrutalism.crm.domain.activity.event.ActivityStatusChangedEvent;
import com.neobrutalism.crm.domain.activity.model.Activity;
import com.neobrutalism.crm.domain.activity.model.ActivityStatus;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import com.neobrutalism.crm.domain.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for Activity management
 * Extends BaseService for standard CRUD operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService extends BaseService<Activity> {

    private final ActivityRepository activityRepository;
    private final EventPublisher eventPublisher;

    @Override
    protected BaseRepository<Activity> getRepository() {
        return activityRepository;
    }

    @Override
    protected String getEntityName() {
        return "Activity";
    }

    /**
     * Create activity from request
     */
    @Transactional
    public Activity create(ActivityRequest request, String createdBy) {
        log.debug("Creating activity: {}", request.getSubject());

        Activity activity = new Activity();
        mapRequestToEntity(request, activity);
        activity.setTenantId(TenantContext.getCurrentTenant());

        beforeCreate(activity);
        Activity saved = activityRepository.save(activity);
        afterCreate(saved);

        // Publish domain event
        eventPublisher.publish(new ActivityCreatedEvent(
                saved.getId().toString(),
                saved.getSubject(),
                saved.getActivityType(),
                saved.getOwnerId(),
                saved.getScheduledStartAt(),
                createdBy
        ));

        log.info("Activity created: {}", saved.getId());
        return saved;
    }

    /**
     * Update activity
     */
    @Transactional
    public Activity update(UUID id, ActivityRequest request, String updatedBy) {
        log.debug("Updating activity: {}", id);

        Activity activity = findById(id);
        mapRequestToEntity(request, activity);

        beforeUpdate(activity);
        Activity updated = activityRepository.save(activity);
        afterUpdate(updated);

        log.info("Activity updated: {}", updated.getId());
        return updated;
    }

    /**
     * Start activity
     */
    @Transactional
    public Activity start(UUID id, String changedBy) {
        log.debug("Starting activity: {}", id);

        Activity activity = findById(id);
        ActivityStatus oldStatus = activity.getStatus();
        activity.start(changedBy);

        Activity updated = activityRepository.save(activity);

        // Publish status changed event
        eventPublisher.publish(new ActivityStatusChangedEvent(
                updated.getId().toString(),
                oldStatus,
                updated.getStatus(),
                "Activity started",
                changedBy
        ));

        log.info("Activity started: {}", updated.getId());
        return updated;
    }

    /**
     * Complete activity
     */
    @Transactional
    public Activity complete(UUID id, String outcome, String changedBy) {
        log.debug("Completing activity: {}", id);

        Activity activity = findById(id);
        activity.complete(changedBy, outcome);

        Activity updated = activityRepository.save(activity);

        // Publish completed event
        eventPublisher.publish(new ActivityCompletedEvent(
                updated.getId().toString(),
                updated.getSubject(),
                updated.getOwnerId(),
                outcome,
                changedBy
        ));

        log.info("Activity completed: {}", updated.getId());
        return updated;
    }

    /**
     * Cancel activity
     */
    @Transactional
    public Activity cancel(UUID id, String reason, String changedBy) {
        log.debug("Cancelling activity: {}", id);

        Activity activity = findById(id);
        activity.cancel(changedBy, reason);

        Activity updated = activityRepository.save(activity);

        // Publish cancelled event
        eventPublisher.publish(new ActivityCancelledEvent(
                updated.getId().toString(),
                updated.getSubject(),
                updated.getOwnerId(),
                reason,
                changedBy
        ));

        log.info("Activity cancelled: {}", updated.getId());
        return updated;
    }

    /**
     * Reschedule activity
     */
    @Transactional
    public Activity reschedule(UUID id, Instant newStartAt, Instant newEndAt, String reason, String changedBy) {
        log.debug("Rescheduling activity: {}", id);

        Activity activity = findById(id);
        ActivityStatus oldStatus = activity.getStatus();
        activity.reschedule(newStartAt, newEndAt, changedBy, reason);

        Activity updated = activityRepository.save(activity);

        // Publish status changed event
        eventPublisher.publish(new ActivityStatusChangedEvent(
                updated.getId().toString(),
                oldStatus,
                updated.getStatus(),
                reason,
                changedBy
        ));

        log.info("Activity rescheduled: {}", updated.getId());
        return updated;
    }

    /**
     * Find activities by owner
     */
    public List<Activity> findByOwner(UUID ownerId) {
        return activityRepository.findByOwnerIdAndDeletedFalse(ownerId);
    }

    /**
     * Find activities by status
     */
    public List<Activity> findByStatus(ActivityStatus status) {
        return activityRepository.findByStatusAndDeletedFalse(status);
    }

    /**
     * Find activities by type
     */
    public List<Activity> findByType(ActivityType type) {
        return activityRepository.findByActivityTypeAndDeletedFalse(type);
    }

    /**
     * Find activities related to an entity
     */
    public List<Activity> findByRelatedEntity(String relatedToType, UUID relatedToId) {
        return activityRepository.findByRelatedToTypeAndRelatedToIdAndDeletedFalse(relatedToType, relatedToId);
    }

    /**
     * Find upcoming activities for user
     */
    public List<Activity> findUpcomingActivities(UUID ownerId, int daysAhead) {
        Instant now = Instant.now();
        Instant endDate = now.plus(daysAhead, ChronoUnit.DAYS);
        return activityRepository.findUpcomingActivities(ownerId, now, endDate);
    }

    /**
     * Find overdue activities
     */
    public List<Activity> findOverdueActivities() {
        return activityRepository.findOverdueActivities(Instant.now());
    }

    /**
     * Count activities by status for user
     */
    public Long countByOwnerAndStatus(UUID ownerId, ActivityStatus status) {
        return activityRepository.countByOwnerAndStatus(ownerId, status);
    }

    /**
     * Find activities by organization
     */
    public List<Activity> findByOrganization(UUID organizationId) {
        return activityRepository.findByOrganizationIdAndDeletedFalse(organizationId);
    }

    /**
     * Find activities by branch
     */
    public List<Activity> findByBranch(UUID branchId) {
        return activityRepository.findByBranchIdAndDeletedFalse(branchId);
    }

    /**
     * Map request DTO to entity
     */
    private void mapRequestToEntity(ActivityRequest request, Activity activity) {
        activity.setSubject(request.getSubject());
        activity.setDescription(request.getDescription());
        activity.setActivityType(request.getActivityType());
        activity.setPriority(request.getPriority());
        activity.setOwnerId(request.getOwnerId());
        activity.setRelatedToType(request.getRelatedToType());
        activity.setRelatedToId(request.getRelatedToId());
        activity.setScheduledStartAt(request.getScheduledStartAt());
        activity.setScheduledEndAt(request.getScheduledEndAt());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setLocation(request.getLocation());
        activity.setIsRecurring(request.getIsRecurring());
        activity.setRecurrencePattern(request.getRecurrencePattern());
        activity.setParentActivityId(request.getParentActivityId());
        activity.setOrganizationId(request.getOrganizationId());
        activity.setBranchId(request.getBranchId());
    }
}
