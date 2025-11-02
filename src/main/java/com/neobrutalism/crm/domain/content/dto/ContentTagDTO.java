package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for ContentTag entity
 */
@Data
@Schema(description = "Content tag data")
public class ContentTagDTO {

    @Schema(description = "Tag ID")
    private UUID id;

    @Schema(description = "Tag name")
    private String name;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Tag color")
    private String color;

    @Schema(description = "Content count")
    private Integer contentCount;
}
