package com.neobrutalism.crm.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for recording command execution.
 *
 * @author Admin
 * @since Phase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandExecutionRequest {
    private String tenantId;
    private UUID userId;
    private UUID commandId;
    private long executionTimeMs;
    private String contextData; // JSON context
}
