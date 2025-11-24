package com.neobrutalism.crm.domain.task.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.common.websocket.WebSocketService;
import com.neobrutalism.crm.domain.task.model.Comment;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.repository.CommentRepository;
import com.neobrutalism.crm.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing task comments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final WebSocketService webSocketService;

    /**
     * Add comment to task
     */
    @Transactional
    public Comment addComment(UUID taskId, UUID userId, String content, UUID parentId) {
        log.info("Adding comment to task: {} by user: {}", taskId, userId);

        // Verify task exists
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Task not found"));

        // Verify parent comment exists if this is a reply
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Parent comment not found"));
            
            if (!parent.getTaskId().equals(taskId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Parent comment belongs to different task");
            }
        }

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setOrganizationId(task.getOrganizationId());

        Comment saved = commentRepository.save(comment);

        // Broadcast comment via WebSocket
        broadcastComment(saved, "COMMENT_ADDED");

        log.info("Comment added: {} to task: {}", saved.getId(), taskId);
        return saved;
    }

    /**
     * Update comment
     */
    @Transactional
    public Comment updateComment(UUID commentId, UUID userId, String newContent) {
        log.info("Updating comment: {} by user: {}", commentId, userId);

        Comment comment = findById(commentId);

        // Verify ownership
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You can only edit your own comments");
        }

        comment.setContent(newContent);
        comment.markAsEdited();

        Comment updated = commentRepository.save(comment);

        // Broadcast update via WebSocket
        broadcastComment(updated, "COMMENT_UPDATED");

        log.info("Comment updated: {}", commentId);
        return updated;
    }

    /**
     * Delete comment (soft delete)
     */
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        log.info("Deleting comment: {} by user: {}", commentId, userId);

        Comment comment = findById(commentId);

        // Verify ownership
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You can only delete your own comments");
        }

        comment.softDelete();
        commentRepository.save(comment);

        // Broadcast deletion via WebSocket
        broadcastComment(comment, "COMMENT_DELETED");

        log.info("Comment deleted: {}", commentId);
    }

    /**
     * Get all comments for a task
     */
    @Transactional(readOnly = true)
    public List<Comment> getComments(UUID taskId) {
        return commentRepository.findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(taskId);
    }

    /**
     * Get comments with pagination
     */
    @Transactional(readOnly = true)
    public Page<Comment> getComments(UUID taskId, Pageable pageable) {
        return commentRepository.findByTaskIdAndDeletedFalse(taskId, pageable);
    }

    /**
     * Get top-level comments (not replies)
     */
    @Transactional(readOnly = true)
    public List<Comment> getTopLevelComments(UUID taskId) {
        return commentRepository.findByTaskIdAndParentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(taskId);
    }

    /**
     * Get replies to a comment
     */
    @Transactional(readOnly = true)
    public List<Comment> getReplies(UUID parentId) {
        return commentRepository.findByParentIdAndDeletedFalseOrderByCreatedAtAsc(parentId);
    }

    /**
     * Count comments for a task
     */
    @Transactional(readOnly = true)
    public long countComments(UUID taskId) {
        return commentRepository.countByTaskIdAndDeletedFalse(taskId);
    }

    /**
     * Find comment by ID
     */
    @Transactional(readOnly = true)
    public Comment findById(UUID commentId) {
        return commentRepository.findById(commentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Comment not found"));
    }

    /**
     * Broadcast comment update via WebSocket
     */
    private void broadcastComment(Comment comment, String action) {
        try {
            String topic = "/topic/tasks/" + comment.getTaskId() + "/comments";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", action);
            payload.put("commentId", comment.getId());
            payload.put("taskId", comment.getTaskId());
            payload.put("userId", comment.getUserId());
            payload.put("content", comment.getContent());
            payload.put("parentId", comment.getParentId());
            payload.put("edited", comment.getEdited());
            payload.put("deleted", comment.getDeleted());
            payload.put("createdAt", comment.getCreatedAt());
            
            webSocketService.sendToTopic(topic, payload);
            
            log.debug("Broadcasted comment {} to topic: {}", action, topic);
        } catch (Exception e) {
            log.error("Failed to broadcast comment update", e);
            // Don't fail the operation if WebSocket fails
        }
    }
}
