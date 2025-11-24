package com.neobrutalism.crm.domain.task.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for reordering checklist items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistReorderRequest {

    @NotEmpty(message = "Item IDs list cannot be empty")
    private List<UUID> itemIds;
}
