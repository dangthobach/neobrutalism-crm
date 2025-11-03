package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for ContentSeries entity
 */
@Data
@Schema(description = "Content series data")
public class ContentSeriesDTO {

    @Schema(description = "Series ID")
    private UUID id;

    @Schema(description = "Series name")
    private String name;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Series description")
    private String description;

    @Schema(description = "Thumbnail URL")
    private String thumbnailUrl;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Content count")
    private Integer contentCount;

    @Schema(description = "Created date")
    private Instant createdAt;

    @Schema(description = "Updated date")
    private Instant updatedAt;
}
