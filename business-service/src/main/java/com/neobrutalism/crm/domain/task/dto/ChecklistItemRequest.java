package com.neobrutalism.crm.domain.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for checklist items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemRequest {

    @NotBlank(message = "Checklist item title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    private Boolean completed;
}
