package com.neobrutalism.crm.domain.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk assigning tasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignRequest {

    @NotEmpty(message = "Task IDs cannot be empty")
    private List<UUID> taskIds;

    @NotNull(message = "Assignee ID is required")
    private UUID assigneeId;
}
