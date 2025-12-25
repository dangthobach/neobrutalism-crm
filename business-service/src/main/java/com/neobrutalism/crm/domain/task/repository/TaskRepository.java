package com.neobrutalism.crm.domain.task.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.common.security.DataScopeHelper;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Task entity
 */
@Repository
public interface TaskRepository extends BaseRepository<Task> {

    List<Task> findByAssignedToIdAndDeletedFalse(UUID assignedToId);

    List<Task> findByAssignedByIdAndDeletedFalse(UUID assignedById);

    List<Task> findByStatusAndDeletedFalse(TaskStatus status);

    List<Task> findByRelatedToTypeAndRelatedToIdAndDeletedFalse(String relatedToType, UUID relatedToId);

    List<Task> findByAssignedToIdAndStatusAndDeletedFalse(UUID assignedToId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.deleted = false " +
           "AND t.dueDate < :now " +
           "AND t.status IN ('TODO', 'IN_PROGRESS')")
    List<Task> findOverdueTasks(@Param("now") Instant now);

    @Query("SELECT t FROM Task t WHERE t.deleted = false " +
           "AND t.assignedToId = :userId " +
           "AND t.dueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.dueDate ASC")
    List<Task> findUpcomingTasks(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    Long countByAssignedToIdAndStatusAndDeletedFalse(UUID assignedToId, TaskStatus status);

    List<Task> findByOrganizationIdAndDeletedFalse(UUID organizationId);

    List<Task> findByBranchIdAndDeletedFalse(UUID branchId);

    // ========================================
    // ✅ PHASE 2: DATA SCOPE ENFORCEMENT
    // ========================================

    /**
     * Find all tasks with data scope filtering
     * Applies row-level security based on user's data scope (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)
     */
    default List<Task> findAllWithScope() {
        return findAll(DataScopeHelper.applyDataScope());
    }

    /**
     * Find all tasks with data scope filtering and pagination
     */
    default Page<Task> findAllWithScope(Pageable pageable) {
        return findAll(DataScopeHelper.applyDataScope(), pageable);
    }

    /**
     * Find tasks assigned to user with data scope filtering
     * Combines assignment filter with row-level security
     */
    default List<Task> findByAssignedToIdWithScope(UUID assignedToId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("assignedToId"), assignedToId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find tasks assigned by user with data scope filtering
     */
    default List<Task> findByAssignedByIdWithScope(UUID assignedById) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("assignedById"), assignedById),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find tasks by status with data scope filtering
     */
    default List<Task> findByStatusWithScope(TaskStatus status) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find tasks by status with data scope filtering and pagination
     */
    default Page<Task> findByStatusWithScope(TaskStatus status, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ), pageable);
    }

    /**
     * Find tasks by assigned user and status with data scope filtering
     */
    default List<Task> findByAssignedToIdAndStatusWithScope(UUID assignedToId, TaskStatus status) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("assignedToId"), assignedToId),
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find tasks by organization with data scope filtering
     */
    default List<Task> findByOrganizationIdWithScope(UUID organizationId) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ));
    }

    /**
     * Find tasks by organization with data scope filtering and pagination
     */
    default Page<Task> findByOrganizationIdWithScope(UUID organizationId, Pageable pageable) {
        return findAll(DataScopeHelper.applyScopeWith(
            DataScopeHelper.byOrganization(organizationId)
        ), pageable);
    }

    /**
     * Find tasks by branch with data scope filtering
     */
    default List<Task> findByBranchIdWithScope(UUID branchId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("branchId"), branchId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find tasks by related entity with data scope filtering
     */
    default List<Task> findByRelatedToTypeAndIdWithScope(String relatedToType, UUID relatedToId) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("relatedToType"), relatedToType),
                cb.equal(root.get("relatedToId"), relatedToId),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find overdue tasks with data scope filtering
     */
    default List<Task> findOverdueTasksWithScope(Instant now) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.lessThan(root.get("dueDate"), now),
                root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Find upcoming tasks for user with data scope filtering
     */
    default List<Task> findUpcomingTasksWithScope(UUID userId, Instant startDate, Instant endDate) {
        return findAll(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> {
                query.orderBy(cb.asc(root.get("dueDate")));
                return cb.and(
                    cb.equal(root.get("assignedToId"), userId),
                    cb.between(root.get("dueDate"), startDate, endDate),
                    cb.isFalse(root.get("deleted"))
                );
            }
        ));
    }

    /**
     * Count tasks by assigned user and status with data scope filtering
     */
    default long countByAssignedToIdAndStatusWithScope(UUID assignedToId, TaskStatus status) {
        return count(DataScopeHelper.applyScopeWith(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("assignedToId"), assignedToId),
                cb.equal(root.get("status"), status),
                cb.isFalse(root.get("deleted"))
            )
        ));
    }

    /**
     * Count tasks with data scope filtering
     */
    default long countWithScope() {
        return count(DataScopeHelper.applyDataScope());
    }
}
