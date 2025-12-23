package com.neobrutalism.crm.domain.task.dto;

import com.neobrutalism.crm.domain.task.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for comments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private UUID taskId;
    private UUID userId;
    private String content;
    private UUID parentId;
    private Boolean edited;
    private Boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .edited(comment.getEdited())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
