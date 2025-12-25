package com.neobrutalism.crm.domain.task.dto;

import com.neobrutalism.crm.domain.task.model.ChecklistItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for checklist items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemResponse {

    private UUID id;
    private UUID taskId;
    private String title;
    private Boolean completed;
    private Integer position;
    private Instant createdAt;
    private Instant updatedAt;

    public static ChecklistItemResponse from(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .taskId(item.getTaskId())
                .title(item.getTitle())
                .completed(item.getCompleted())
                .position(item.getPosition())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
