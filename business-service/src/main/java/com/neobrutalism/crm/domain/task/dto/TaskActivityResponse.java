package com.neobrutalism.crm.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for task activity timeline entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivityResponse {

    private UUID id;
    
    private UUID taskId;
    
    private String activityType; // CREATED, STATUS_CHANGED, ASSIGNED, COMMENT_ADDED, CHECKLIST_UPDATED, ATTACHMENT_ADDED
    
    private String description;
    
    private UUID userId;
    
    private String username;
    
    private Map<String, Object> metadata; // Additional data (e.g., old/new values)
    
    private Instant createdAt;
}
