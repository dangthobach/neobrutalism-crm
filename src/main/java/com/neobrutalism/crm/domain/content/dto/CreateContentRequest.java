package com.neobrutalism.crm.domain.content.dto;

import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for creating content
 */
@Data
@Schema(description = "Create content request")
public class CreateContentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be at most 500 characters")
    @Schema(description = "Content title", example = "Getting Started with Spring Boot")
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 500, message = "Slug must be at most 500 characters")
    @Schema(description = "URL-friendly slug", example = "getting-started-with-spring-boot")
    private String slug;

    @Size(max = 2000, message = "Summary must be at most 2000 characters")
    @Schema(description = "Content summary/excerpt")
    private String summary;

    @NotBlank(message = "Body is required")
    @Schema(description = "Content body (HTML)")
    private String body;

    @Schema(description = "Content type", example = "BLOG")
    private ContentType contentType = ContentType.BLOG;

    @Schema(description = "Featured image ID")
    private UUID featuredImageId;

    @Schema(description = "Minimum tier required to access", example = "FREE")
    private MemberTier tierRequired = MemberTier.FREE;

    @Schema(description = "Series ID (if part of a series)")
    private UUID seriesId;

    @Schema(description = "Category IDs")
    private Set<UUID> categoryIds;

    @Schema(description = "Tag IDs")
    private Set<UUID> tagIds;

    @Size(max = 255, message = "SEO title must be at most 255 characters")
    @Schema(description = "SEO title (overrides main title)")
    private String seoTitle;

    @Size(max = 500, message = "SEO description must be at most 500 characters")
    @Schema(description = "SEO meta description")
    private String seoDescription;

    @Size(max = 500, message = "SEO keywords must be at most 500 characters")
    @Schema(description = "SEO keywords (comma-separated)")
    private String seoKeywords;
}
