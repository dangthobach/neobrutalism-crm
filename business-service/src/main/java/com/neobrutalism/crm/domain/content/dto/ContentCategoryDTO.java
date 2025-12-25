package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for ContentCategory entity
 */
@Data
@Schema(description = "Content category data")
public class ContentCategoryDTO {

    @Schema(description = "Category ID")
    private UUID id;

    @Schema(description = "Category name")
    private String name;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Parent category ID")
    private UUID parentId;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Content count")
    private Integer contentCount;

    @Schema(description = "Created date")
    private Instant createdAt;

    @Schema(description = "Updated date")
    private Instant updatedAt;
}
