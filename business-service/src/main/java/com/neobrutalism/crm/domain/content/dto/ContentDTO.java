package com.neobrutalism.crm.domain.content.dto;

import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for Content entity
 */
@Data
@Schema(description = "Content response data")
public class ContentDTO {

    @Schema(description = "Content ID")
    private UUID id;

    @Schema(description = "Tenant ID")
    private String tenantId;

    @Schema(description = "Content title")
    private String title;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Content summary")
    private String summary;

    @Schema(description = "Content body (HTML)")
    private String body;

    @Schema(description = "Content type")
    private ContentType contentType;

    @Schema(description = "Content status")
    private ContentStatus status;

    @Schema(description = "Published date")
    private Instant publishedAt;

    @Schema(description = "View count")
    private Integer viewCount;

    @Schema(description = "Minimum tier required")
    private MemberTier tierRequired;

    @Schema(description = "Featured image URL")
    private String featuredImageUrl;

    @Schema(description = "Author information")
    private AuthorDTO author;

    @Schema(description = "Series information")
    private ContentSeriesDTO series;

    @Schema(description = "Categories")
    private Set<ContentCategoryDTO> categories;

    @Schema(description = "Tags")
    private Set<ContentTagDTO> tags;

    @Schema(description = "SEO title")
    private String seoTitle;

    @Schema(description = "SEO description")
    private String seoDescription;

    @Schema(description = "SEO keywords")
    private String seoKeywords;

    @Schema(description = "Created date")
    private Instant createdAt;

    @Schema(description = "Updated date")
    private Instant updatedAt;

    @Schema(description = "Version (for optimistic locking)")
    private Long version;

    @Data
    @Schema(description = "Author information")
    public static class AuthorDTO {
        private UUID id;
        private String fullName;
        private String email;
        private String avatarUrl;
    }
}
