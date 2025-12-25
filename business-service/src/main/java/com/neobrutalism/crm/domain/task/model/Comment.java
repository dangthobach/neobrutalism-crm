package com.neobrutalism.crm.domain.task.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Comment entity for task discussions
 * Supports threaded comments (replies)
 */
@Entity
@Table(
    name = "task_comments",
    indexes = {
        @Index(name = "idx_comment_task", columnList = "task_id"),
        @Index(name = "idx_comment_user", columnList = "user_id"),
        @Index(name = "idx_comment_parent", columnList = "parent_id"),
        @Index(name = "idx_comment_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Comment extends TenantAwareEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank
    @Size(max = 5000)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "edited", nullable = false)
    private Boolean edited = false;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * Mark comment as edited
     */
    public void markAsEdited() {
        this.edited = true;
    }

    /**
     * Soft delete comment
     */
    public void softDelete() {
        this.deleted = true;
        this.content = "[deleted]";
    }

    /**
     * Check if this is a reply to another comment
     */
    public boolean isReply() {
        return parentId != null;
    }
}
