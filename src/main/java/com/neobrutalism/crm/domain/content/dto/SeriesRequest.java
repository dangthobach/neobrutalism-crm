package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for ContentSeries operations
 */
@Data
@Schema(description = "Content series request")
public class SeriesRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    @Schema(description = "Series name", example = "Mastering Spring Framework")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 200, message = "Slug must be at most 200 characters")
    @Schema(description = "URL-friendly slug", example = "mastering-spring-framework")
    private String slug;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    @Schema(description = "Series description")
    private String description;

    @Schema(description = "Thumbnail image ID")
    private UUID thumbnailId;

    @Schema(description = "Sort order", example = "0")
    private Integer sortOrder = 0;
}
