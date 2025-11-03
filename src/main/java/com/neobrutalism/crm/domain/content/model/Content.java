package com.neobrutalism.crm.domain.content.model;

import com.neobrutalism.crm.common.entity.StatefulEntity;
import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Content entity for CMS (blog posts, articles, pages, etc.)
 * Uses CQRS pattern - this is the write model
 */
@Entity
@Table(name = "contents", indexes = {
    @Index(name = "idx_contents_tenant", columnList = "tenant_id"),
    @Index(name = "idx_contents_slug", columnList = "slug"),
    @Index(name = "idx_contents_status", columnList = "status"),
    @Index(name = "idx_contents_author", columnList = "author_id"),
    @Index(name = "idx_contents_published_at", columnList = "published_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Content extends StatefulEntity<ContentStatus> {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "slug", nullable = false, length = 500)
    private String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "featured_image_id")
    private Attachment featuredImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ContentType contentType = ContentType.BLOG;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_required", length = 50)
    private MemberTier tierRequired = MemberTier.FREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private ContentSeries series;

    @Column(name = "series_order")
    private Integer seriesOrder = 0;

    // SEO fields
    @Column(name = "seo_title", length = 255)
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    @Column(name = "seo_keywords", columnDefinition = "TEXT")
    private String seoKeywords;

    // Many-to-Many relationships
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "content_category_mappings",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<ContentCategory> categories = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "content_tag_mappings",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ContentTag> tags = new HashSet<>();

    @Override
    protected ContentStatus getInitialStatus() {
        return ContentStatus.DRAFT;
    }

    @Override
    protected Set<ContentStatus> getAllowedTransitions(ContentStatus currentStatus) {
        return switch (currentStatus) {
            case DRAFT -> Set.of(ContentStatus.REVIEW, ContentStatus.DELETED);
            case REVIEW -> Set.of(ContentStatus.DRAFT, ContentStatus.PUBLISHED, ContentStatus.DELETED);
            case PUBLISHED -> Set.of(ContentStatus.DRAFT, ContentStatus.ARCHIVED, ContentStatus.DELETED);
            case ARCHIVED -> Set.of(ContentStatus.PUBLISHED, ContentStatus.DELETED);
            case DELETED -> Set.of(); // No transitions from deleted
        };
    }

    @Override
    protected void onStatusChanged(ContentStatus oldStatus, ContentStatus newStatus) {
        if (newStatus == ContentStatus.PUBLISHED && oldStatus != ContentStatus.PUBLISHED) {
            this.publishedAt = Instant.now();
        }
    }

    // Business methods

    /**
     * Submit content for review
     */
    public void submitForReview(String reviewedBy, String reason) {
        transitionTo(ContentStatus.REVIEW, reviewedBy, reason);
    }

    /**
     * Publish content
     */
    public void publish(String publishedBy, String reason) {
        transitionTo(ContentStatus.PUBLISHED, publishedBy, reason);
    }

    /**
     * Archive content
     */
    public void archive(String archivedBy, String reason) {
        transitionTo(ContentStatus.ARCHIVED, archivedBy, reason);
    }

    /**
     * Return to draft
     */
    public void returnToDraft(String changedBy, String reason) {
        transitionTo(ContentStatus.DRAFT, changedBy, reason);
    }

    /**
     * Increment view count
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * Add category
     */
    public void addCategory(ContentCategory category) {
        this.categories.add(category);
    }

    /**
     * Remove category
     */
    public void removeCategory(ContentCategory category) {
        this.categories.remove(category);
    }

    /**
     * Add tag
     */
    public void addTag(ContentTag tag) {
        this.tags.add(tag);
    }

    /**
     * Remove tag
     */
    public void removeTag(ContentTag tag) {
        this.tags.remove(tag);
    }

    /**
     * Check if content is published
     */
    public boolean isPublished() {
        return this.getStatus() == ContentStatus.PUBLISHED;
    }

    /**
     * Check if content requires specific tier
     */
    public boolean requiresTier(MemberTier tier) {
        return this.tierRequired.isHigherOrEqualTo(tier);
    }

    /**
     * Check if user can access this content based on their tier
     */
    public boolean canBeAccessedBy(MemberTier userTier) {
        return userTier.canAccess(this.tierRequired);
    }
}
