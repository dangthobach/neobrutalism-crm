package com.neobrutalism.crm.domain.activity.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.activity.model.Activity;
import com.neobrutalism.crm.domain.activity.model.ActivityStatus;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Activity entity
 * Extends BaseRepository for standard CRUD and Specification support
 */
@Repository
public interface ActivityRepository extends BaseRepository<Activity> {

    /**
     * Find activities by owner
     */
    List<Activity> findByOwnerIdAndDeletedFalse(UUID ownerId);

    /**
     * Find activities by status
     */
    List<Activity> findByStatusAndDeletedFalse(ActivityStatus status);

    /**
     * Find activities by type
     */
    List<Activity> findByActivityTypeAndDeletedFalse(ActivityType activityType);

    /**
     * Find activities related to an entity
     */
    List<Activity> findByRelatedToTypeAndRelatedToIdAndDeletedFalse(String relatedToType, UUID relatedToId);

    /**
     * Find activities scheduled between dates
     */
    List<Activity> findByScheduledStartAtBetweenAndDeletedFalse(Instant startDate, Instant endDate);

    /**
     * Find activities by owner and status
     */
    List<Activity> findByOwnerIdAndStatusAndDeletedFalse(UUID ownerId, ActivityStatus status);

    /**
     * Find overdue activities
     */
    @Query("SELECT a FROM Activity a WHERE a.deleted = false " +
           "AND a.scheduledEndAt < :now " +
           "AND a.status IN ('PLANNED', 'IN_PROGRESS')")
    List<Activity> findOverdueActivities(@Param("now") Instant now);

    /**
     * Find upcoming activities for user
     */
    @Query("SELECT a FROM Activity a WHERE a.deleted = false " +
           "AND a.ownerId = :ownerId " +
           "AND a.scheduledStartAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.scheduledStartAt ASC")
    List<Activity> findUpcomingActivities(
            @Param("ownerId") UUID ownerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Count activities by status for user
     */
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.deleted = false " +
           "AND a.ownerId = :ownerId AND a.status = :status")
    Long countByOwnerAndStatus(
            @Param("ownerId") UUID ownerId,
            @Param("status") ActivityStatus status
    );

    /**
     * Find activities by organization
     */
    List<Activity> findByOrganizationIdAndDeletedFalse(UUID organizationId);

    /**
     * Find activities by branch
     */
    List<Activity> findByBranchIdAndDeletedFalse(UUID branchId);
}
