package com.neobrutalism.crm.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for bulk operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse {

    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<UUID> successfulTaskIds;
    private List<BulkOperationError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkOperationError {
        private UUID taskId;
        private String reason;
    }
}
