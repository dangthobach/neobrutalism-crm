package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for ContentCategory operations
 */
@Data
@Schema(description = "Content category request")
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    @Schema(description = "Category name", example = "Technology")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 200, message = "Slug must be at most 200 characters")
    @Schema(description = "URL-friendly slug", example = "technology")
    private String slug;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Parent category ID")
    private UUID parentId;

    @Schema(description = "Sort order", example = "0")
    private Integer sortOrder = 0;
}
