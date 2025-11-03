package com.neobrutalism.crm.domain.task.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.model.TaskStatus;
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
}
