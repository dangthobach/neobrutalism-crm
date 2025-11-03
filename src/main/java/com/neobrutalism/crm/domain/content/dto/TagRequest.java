package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for ContentTag operations
 */
@Data
@Schema(description = "Content tag request")
public class TagRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @Schema(description = "Tag name", example = "Spring Boot")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 100, message = "Slug must be at most 100 characters")
    @Schema(description = "URL-friendly slug", example = "spring-boot")
    private String slug;

    @Size(max = 20, message = "Color must be at most 20 characters")
    @Schema(description = "Tag color (hex)", example = "#3B82F6")
    private String color;
}
