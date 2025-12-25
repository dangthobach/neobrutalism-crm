package com.neobrutalism.crm.domain.content.dto;

import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for updating content
 */
@Data
@Schema(description = "Update content request")
public class UpdateContentRequest {

    @Size(max = 500, message = "Title must be at most 500 characters")
    @Schema(description = "Content title")
    private String title;

    @Size(max = 500, message = "Slug must be at most 500 characters")
    @Schema(description = "URL-friendly slug")
    private String slug;

    @Size(max = 2000, message = "Summary must be at most 2000 characters")
    @Schema(description = "Content summary/excerpt")
    private String summary;

    @Schema(description = "Content body (HTML)")
    private String body;

    @Schema(description = "Content type")
    private ContentType contentType;

    @Schema(description = "Featured image ID")
    private UUID featuredImageId;

    @Schema(description = "Minimum tier required to access")
    private MemberTier tierRequired;

    @Schema(description = "Series ID (if part of a series)")
    private UUID seriesId;

    @Schema(description = "Category IDs")
    private Set<UUID> categoryIds;

    @Schema(description = "Tag IDs")
    private Set<UUID> tagIds;

    @Size(max = 255, message = "SEO title must be at most 255 characters")
    @Schema(description = "SEO title")
    private String seoTitle;

    @Size(max = 500, message = "SEO description must be at most 500 characters")
    @Schema(description = "SEO meta description")
    private String seoDescription;

    @Size(max = 500, message = "SEO keywords must be at most 500 characters")
    @Schema(description = "SEO keywords (comma-separated)")
    private String seoKeywords;
}
