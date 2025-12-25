package com.neobrutalism.crm.domain.task.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.task.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends BaseRepository<Comment> {
    
    /**
     * Find all comments for a task, ordered by creation date
     */
    List<Comment> findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(UUID taskId);

    /**
     * Find all comments for a task with pagination
     */
    Page<Comment> findByTaskIdAndDeletedFalse(UUID taskId, Pageable pageable);

    /**
     * Find top-level comments (not replies)
     */
    List<Comment> findByTaskIdAndParentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(UUID taskId);

    /**
     * Find replies to a comment
     */
    List<Comment> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(UUID parentId);

    /**
     * Count comments for a task
     */
    long countByTaskIdAndDeletedFalse(UUID taskId);

    /**
     * Find comments by user
     */
    List<Comment> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Check if user owns a comment
     */
    boolean existsByIdAndUserId(UUID commentId, UUID userId);
}
