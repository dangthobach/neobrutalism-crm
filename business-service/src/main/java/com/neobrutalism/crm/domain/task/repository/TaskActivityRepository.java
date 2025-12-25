package com.neobrutalism.crm.domain.task.repository;

import com.neobrutalism.crm.domain.task.model.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for TaskActivity entity
 */
@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID> {

    /**
     * Find all activities for a task, ordered by creation time descending
     */
    List<TaskActivity> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    /**
     * Find activities for a task with pagination
     */
    Page<TaskActivity> findByTaskIdOrderByCreatedAtDesc(UUID taskId, Pageable pageable);

    /**
     * Find activities by type for a task
     */
    List<TaskActivity> findByTaskIdAndActivityTypeOrderByCreatedAtDesc(UUID taskId, String activityType);

    /**
     * Find activities by user
     */
    List<TaskActivity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Count activities for a task
     */
    long countByTaskId(UUID taskId);

    /**
     * Delete all activities for a task (used when task is hard deleted)
     */
    void deleteByTaskId(UUID taskId);
}
