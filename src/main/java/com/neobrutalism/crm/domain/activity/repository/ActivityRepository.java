package com.neobrutalism.crm.domain.activity.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.common.security.DataScopeHelper;
import com.neobrutalism.crm.domain.activity.model.Activity;
import com.neobrutalism.crm.domain.activity.model.ActivityStatus;
import com.neobrutalism.crm.domain.activity.model.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ========================================
    // ✅ PHASE 2: DATA SCOPE ENFORCEMENT
    // ========================================

    /**
     * Find all activities with data scope filtering
     * Applies row-level security based on user's data scope (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
     */
    default List<Activity> findAllWithScope() {
        return findAll(DataScopeHelper.applyDataScope());
    }

    /**
     * Find all activities with data scope filtering and pagination
     */
    default Page<Activity> findAllWithScope(Pageable pageable) {
        return findAll(DataScopeHelper.applyDataScope(), pageable);
    }

    /**
     * Find activities by owner with data scope filtering
     */
    default List<Activity> findByOwnerIdWithScope(UUID ownerId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("ownerId"), ownerId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find activities by status with data scope filtering
     */
    default List<Activity> findByStatusWithScope(ActivityStatus status) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find activities by status with data scope filtering and pagination
     */
    default Page<Activity> findByStatusWithScope(ActivityStatus status, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ), pageable);
    }

    /**
     * Find activities by type with data scope filtering
     */
    default List<Activity> findByActivityTypeWithScope(ActivityType activityType) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("activityType"), activityType),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find activities by owner and status with data scope filtering
     */
    default List<Activity> findByOwnerIdAndStatusWithScope(UUID ownerId, ActivityStatus status) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("ownerId"), ownerId),
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find activities by related entity with data scope filtering
     */
    default List<Activity> findByRelatedToTypeAndIdWithScope(String relatedToType, UUID relatedToId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("relatedToType"), relatedToType),
                cb.equal(root.get("relatedToId"), relatedToId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find activities scheduled between dates with data scope filtering
     */
    default List<Activity> findByScheduledBetweenWithScope(Instant startDate, Instant endDate) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.between(root.get("scheduledStartAt"), startDate, endDate),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find overdue activities with data scope filtering
     */
    default List<Activity> findOverdueActivitiesWithScope(Instant now) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.lessThan(root.get("scheduledEndAt"), now),
                root.get("status").in(ActivityStatus.PLANNED, ActivityStatus.IN_PROGRESS),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find upcoming activities for user with data scope filtering
     */
    default List<Activity> findUpcomingActivitiesWithScope(UUID ownerId, Instant startDate, Instant endDate) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> {
                query.orderBy(cb.asc(root.get("scheduledStartAt")));
                return cb.and(
                    cb.equal(root.get("ownerId"), ownerId),
                    cb.between(root.get("scheduledStartAt"), startDate, endDate),
                    cb.isFalse(root.get("deleted"))
                );
            }
        ));
    }

    /**
     * Find activities by organization with data scope filtering
     */
    default List<Activity> findByOrganizationIdWithScope(UUID organizationId) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ));
    }

    /**
     * Find activities by organization with data scope filtering and pagination
     */
    default Page<Activity> findByOrganizationIdWithScope(UUID organizationId, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ), pageable);
    }

    /**
     * Find activities by branch with data scope filtering
     */
    default List<Activity> findByBranchIdWithScope(UUID branchId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("branchId"), branchId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Count activities by owner and status with data scope filtering
     */
    default long countByOwnerAndStatusWithScope(UUID ownerId, ActivityStatus status) {
        return count(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("ownerId"), ownerId),
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Count activities with data scope filtering
     */
    default long countWithScope() {
        return count(DataScopeHelper.applyDataScope());
    }
}
