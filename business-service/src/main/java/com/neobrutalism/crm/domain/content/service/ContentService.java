package com.neobrutalism.crm.domain.content.service;

import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.service.StatefulService;
import com.neobrutalism.crm.domain.content.dto.*;
import com.neobrutalism.crm.domain.content.event.*;
import com.neobrutalism.crm.domain.content.model.*;
import com.neobrutalism.crm.domain.content.repository.*;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.repository.AttachmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for Content entity
 */
@Service
@Slf4j
public class ContentService extends StatefulService<Content, ContentStatus> {

    private final ContentRepository contentRepository;
    private final ContentCategoryRepository categoryRepository;
    private final ContentTagRepository tagRepository;
    private final ContentSeriesRepository seriesRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ContentService(ContentRepository contentRepository,
                         ContentCategoryRepository categoryRepository,
                         ContentTagRepository tagRepository,
                         ContentSeriesRepository seriesRepository,
                         UserRepository userRepository,
                         AttachmentRepository attachmentRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.contentRepository = contentRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.seriesRepository = seriesRepository;
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected ContentRepository getRepository() {
        return contentRepository;
    }

    @Override
    protected String getEntityName() {
        return "Content";
    }

    /**
     * Create new content
     */
    @Transactional
    public Content createContent(CreateContentRequest request, UUID authorId, String tenantId) {
        log.info("Creating content: {} by author: {}", request.getTitle(), authorId);

        // Check slug uniqueness
        if (contentRepository.existsByTenantIdAndSlugAndDeletedFalse(tenantId, request.getSlug())) {
            throw new IllegalArgumentException("Content with slug '" + request.getSlug() + "' already exists");
        }

        // Find author
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new IllegalArgumentException("Author not found"));

        Content content = new Content();
        content.setTenantId(tenantId);
        content.setTitle(request.getTitle());
        content.setSlug(request.getSlug());
        content.setSummary(request.getSummary());
        content.setBody(request.getBody());
        content.setContentType(request.getContentType());
        content.setTierRequired(request.getTierRequired());
        content.setAuthor(author);

        // Set featured image
        if (request.getFeaturedImageId() != null) {
            Attachment featuredImage = attachmentRepository.findById(request.getFeaturedImageId())
                .orElse(null);
            content.setFeaturedImage(featuredImage);
        }

        // Set series
        if (request.getSeriesId() != null) {
            ContentSeries series = seriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new IllegalArgumentException("Series not found"));
            content.setSeries(series);
        }

        // Set categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<ContentCategory> categories = new HashSet<>(
                categoryRepository.findAllById(request.getCategoryIds())
            );
            content.setCategories(categories);
        }

        // Set tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<ContentTag> tags = new HashSet<>(
                tagRepository.findAllById(request.getTagIds())
            );
            content.setTags(tags);
        }

        // SEO fields
        content.setSeoTitle(request.getSeoTitle());
        content.setSeoDescription(request.getSeoDescription());
        content.setSeoKeywords(request.getSeoKeywords());

        Content saved = create(content);

        // Publish event
        eventPublisher.publishEvent(new ContentCreatedEvent(
            saved.getId(),
            saved.getTitle(),
            saved.getSlug(),
            saved.getContentType(),
            authorId,
            tenantId,
            getCurrentUser()
        ));

        log.info("Content created successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Update content
     */
    @Transactional
    public Content updateContent(UUID id, UpdateContentRequest request) {
        log.info("Updating content: {}", id);

        Content content = findById(id);

        // Check if content can be edited
        if (!content.getStatus().isEditable()) {
            throw new IllegalStateException("Content in status " + content.getStatus() + " cannot be edited");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            content.setTitle(request.getTitle());
        }

        if (request.getSlug() != null && !request.getSlug().equals(content.getSlug())) {
            // Check new slug uniqueness
            if (contentRepository.existsByTenantIdAndSlugAndDeletedFalse(content.getTenantId(), request.getSlug())) {
                throw new IllegalArgumentException("Content with slug '" + request.getSlug() + "' already exists");
            }
            content.setSlug(request.getSlug());
        }

        if (request.getSummary() != null) {
            content.setSummary(request.getSummary());
        }

        if (request.getBody() != null) {
            content.setBody(request.getBody());
        }

        if (request.getContentType() != null) {
            content.setContentType(request.getContentType());
        }

        if (request.getTierRequired() != null) {
            content.setTierRequired(request.getTierRequired());
        }

        if (request.getFeaturedImageId() != null) {
            Attachment featuredImage = attachmentRepository.findById(request.getFeaturedImageId())
                .orElse(null);
            content.setFeaturedImage(featuredImage);
        }

        if (request.getSeriesId() != null) {
            ContentSeries series = seriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new IllegalArgumentException("Series not found"));
            content.setSeries(series);
        }

        if (request.getCategoryIds() != null) {
            Set<ContentCategory> categories = new HashSet<>(
                categoryRepository.findAllById(request.getCategoryIds())
            );
            content.setCategories(categories);
        }

        if (request.getTagIds() != null) {
            Set<ContentTag> tags = new HashSet<>(
                tagRepository.findAllById(request.getTagIds())
            );
            content.setTags(tags);
        }

        if (request.getSeoTitle() != null) {
            content.setSeoTitle(request.getSeoTitle());
        }

        if (request.getSeoDescription() != null) {
            content.setSeoDescription(request.getSeoDescription());
        }

        if (request.getSeoKeywords() != null) {
            content.setSeoKeywords(request.getSeoKeywords());
        }

        Content updated = update(id, content);

        // Publish event
        eventPublisher.publishEvent(new ContentUpdatedEvent(
            updated.getId(),
            updated.getTitle(),
            updated.getSlug(),
            updated.getTenantId(),
            getCurrentUser()
        ));

        log.info("Content updated successfully: {}", id);
        return updated;
    }

    /**
     * Publish content
     */
    @Transactional
    public Content publishContent(UUID id, String reason) {
        log.info("Publishing content: {}", id);

        Content content = findById(id);
        ContentStatus oldStatus = content.getStatus();

        content.publish(getCurrentUser(), reason);
        Content published = contentRepository.save(content);

        // Record state transition
        recordStateTransition(content, oldStatus, ContentStatus.PUBLISHED, reason);

        // Publish event
        eventPublisher.publishEvent(new ContentPublishedEvent(
            published.getId(),
            published.getTitle(),
            published.getSlug(),
            published.getAuthor().getId(),
            published.getPublishedAt(),
            published.getTenantId(),
            getCurrentUser()
        ));

        // Publish status changed event
        eventPublisher.publishEvent(new ContentStatusChangedEvent(
            published.getId(),
            published.getTitle(),
            oldStatus,
            ContentStatus.PUBLISHED,
            reason,
            published.getTenantId(),
            getCurrentUser()
        ));

        log.info("Content published successfully: {}", id);
        return published;
    }

    /**
     * Submit content for review
     */
    @Transactional
    public Content submitForReview(UUID id, String reason) {
        log.info("Submitting content for review: {}", id);

        Content content = findById(id);
        ContentStatus oldStatus = content.getStatus();

        content.submitForReview(getCurrentUser(), reason);
        Content updated = contentRepository.save(content);

        // Record state transition
        recordStateTransition(content, oldStatus, ContentStatus.REVIEW, reason);

        // Publish status changed event
        eventPublisher.publishEvent(new ContentStatusChangedEvent(
            updated.getId(),
            updated.getTitle(),
            oldStatus,
            ContentStatus.REVIEW,
            reason,
            updated.getTenantId(),
            getCurrentUser()
        ));

        log.info("Content submitted for review: {}", id);
        return updated;
    }

    /**
     * Archive content
     */
    @Transactional
    public Content archiveContent(UUID id, String reason) {
        log.info("Archiving content: {}", id);

        Content content = findById(id);
        ContentStatus oldStatus = content.getStatus();

        content.archive(getCurrentUser(), reason);
        Content archived = contentRepository.save(content);

        // Record state transition
        recordStateTransition(content, oldStatus, ContentStatus.ARCHIVED, reason);

        // Publish status changed event
        eventPublisher.publishEvent(new ContentStatusChangedEvent(
            archived.getId(),
            archived.getTitle(),
            oldStatus,
            ContentStatus.ARCHIVED,
            reason,
            archived.getTenantId(),
            getCurrentUser()
        ));

        log.info("Content archived: {}", id);
        return archived;
    }

    /**
     * Find content by slug
     */
    @Transactional(readOnly = true)
    public Content findBySlug(String slug, String tenantId) {
        return contentRepository.findByTenantIdAndSlugAndDeletedFalse(tenantId, slug)
            .orElseThrow(() -> new IllegalArgumentException("Content not found with slug: " + slug));
    }

    /**
     * Find published content
     */
    @Transactional(readOnly = true)
    public Page<Content> findPublishedContent(Pageable pageable) {
        return contentRepository.findByStatusAndDeletedFalse(ContentStatus.PUBLISHED, pageable);
    }

    /**
     * Find content by category
     */
    @Transactional(readOnly = true)
    public Page<Content> findByCategory(UUID categoryId, Pageable pageable) {
        return contentRepository.findByCategoryId(categoryId, pageable);
    }

    /**
     * Find content by tag
     */
    @Transactional(readOnly = true)
    public Page<Content> findByTag(UUID tagId, Pageable pageable) {
        return contentRepository.findByTagId(tagId, pageable);
    }

    /**
     * Search content
     */
    @Transactional(readOnly = true)
    public Page<Content> searchContent(String keyword, Pageable pageable) {
        return contentRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Find trending content
     */
    @Transactional(readOnly = true)
    public Page<Content> findTrending(Instant since, Pageable pageable) {
        return contentRepository.findTrending(since, pageable);
    }

    /**
     * Increment view count (called by view tracking service)
     */
    @Transactional
    public void incrementViewCount(UUID contentId) {
        contentRepository.incrementViewCount(contentId);
    }
}
