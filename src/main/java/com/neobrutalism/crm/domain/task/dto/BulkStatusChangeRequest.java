package com.neobrutalism.crm.domain.task.dto;

import com.neobrutalism.crm.domain.task.model.TaskStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk changing task status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusChangeRequest {

    @NotEmpty(message = "Task IDs cannot be empty")
    private List<UUID> taskIds;

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
