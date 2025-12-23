package com.neobrutalism.crm.domain.task.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.task.model.ChecklistItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends BaseRepository<ChecklistItem> {
    
    /**
     * Find all checklist items for a task, ordered by position
     */
    List<ChecklistItem> findByTaskIdAndDeletedFalseOrderByPositionAsc(UUID taskId);

    /**
     * Count total items for a task
     */
    long countByTaskIdAndDeletedFalse(UUID taskId);

    /**
     * Count completed items for a task
     */
    long countByTaskIdAndCompletedAndDeletedFalse(UUID taskId, Boolean completed);

    /**
     * Get max position for a task (for appending new items)
     */
    @Query("SELECT COALESCE(MAX(c.position), 0) FROM ChecklistItem c WHERE c.taskId = :taskId AND c.deleted = false")
    Integer getMaxPosition(@Param("taskId") UUID taskId);

    /**
     * Delete all items for a task
     */
    void deleteByTaskId(UUID taskId);
}
