package com.neobrutalism.crm.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for command search.
 *
 * @author Admin
 * @since Phase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandSearchResponse {
    private List<CommandDto> commands;
    private int totalCount;
    private boolean hasMore;
}
