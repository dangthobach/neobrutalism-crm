package com.neobrutalism.crm.domain.content.model;

import com.neobrutalism.crm.common.cqrs.ReadModel;
import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Content read model for CQRS pattern
 * Denormalized view optimized for queries
 */
@Entity
@Table(name = "content_read_models", indexes = {
    @Index(name = "idx_content_read_models_tenant", columnList = "tenant_id"),
    @Index(name = "idx_content_read_models_status", columnList = "status"),
    @Index(name = "idx_content_read_models_published_at", columnList = "published_at"),
    @Index(name = "idx_content_read_models_author", columnList = "author_id"),
    @Index(name = "idx_content_read_models_tier", columnList = "tier_required")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ReadModel(aggregate = "Content", description = "Denormalized content view optimized for queries")
public class ContentReadModel {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "slug", nullable = false, length = 500)
    private String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "featured_image_url", length = 1000)
    private String featuredImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_required", length = 50)
    private MemberTier tierRequired;

    // Denormalized author information
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(name = "author_avatar_url", length = 1000)
    private String authorAvatarUrl;

    // Denormalized category and tag names for search
    @Column(name = "category_names", columnDefinition = "TEXT")
    private String categoryNames; // Comma-separated

    @Column(name = "tag_names", columnDefinition = "TEXT")
    private String tagNames; // Comma-separated

    // SEO fields
    @Column(name = "seo_title", length = 255)
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Helper methods

    /**
     * Check if content is published
     */
    public boolean isPublished() {
        return this.status == ContentStatus.PUBLISHED;
    }

    /**
     * Check if user can access based on tier
     */
    public boolean canBeAccessedBy(MemberTier userTier) {
        return userTier.canAccess(this.tierRequired);
    }
}
