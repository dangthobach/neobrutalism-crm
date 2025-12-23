package com.neobrutalism.crm.application.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for migration errors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationErrorResponse {
    
    private UUID jobId;
    private UUID sheetId;
    private String sheetName;
    private long totalErrors;
    private List<ErrorDetail> errors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private UUID id;
        private Long rowNumber;
        private Integer batchNumber;
        private String errorCode;
        private String errorMessage;
        private String validationRule;
        private Object errorData;
        private Instant createdAt;
    }
}

