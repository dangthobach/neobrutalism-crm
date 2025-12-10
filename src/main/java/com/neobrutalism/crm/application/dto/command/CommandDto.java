package com.neobrutalism.crm.application.dto.command;

import com.neobrutalism.crm.domain.command.model.CommandCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for Command entity.
 *
 * @author Admin
 * @since Phase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandDto {
    private UUID id;
    private String commandId;
    private String label;
    private String description;
    private CommandCategory category;
    private String icon;
    private String shortcutKey;
    private String actionType;
    private String actionPayload;
    private String requiredPermission;
    private Long executionCount;
    private Long avgExecutionTimeMs;
}
