package com.neobrutalism.crm.application.dto.command;

import com.neobrutalism.crm.domain.command.model.CommandCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching commands.
 *
 * @author Admin
 * @since Phase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandSearchRequest {
    private String tenantId;
    private String userId;
    private String query;
    private CommandCategory category;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 50;
}
