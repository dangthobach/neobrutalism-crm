package com.neobrutalism.crm.domain.content.service;

import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.content.model.Content;
import com.neobrutalism.crm.domain.content.model.ContentReadModel;
import com.neobrutalism.crm.domain.content.repository.ContentReadModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ContentReadModel (CQRS Read Model)
 * Handles queries optimized for read operations
 */
@Service
@Slf4j
public class ContentReadModelService {

    private final ContentReadModelRepository readModelRepository;

    public ContentReadModelService(ContentReadModelRepository readModelRepository) {
        this.readModelRepository = readModelRepository;
    }

    /**
     * Synchronize read model from write model
     * Called by event handlers when content is created/updated
     */
    @Transactional
    public ContentReadModel syncFromContent(Content content) {
        log.debug("Syncing read model for content: {}", content.getId());

        ContentReadModel readModel = readModelRepository.findById(content.getId())
            .orElse(new ContentReadModel());

        readModel.setId(content.getId());
        readModel.setTenantId(content.getTenantId());
        readModel.setTitle(content.getTitle());
        readModel.setSlug(content.getSlug());
        readModel.setSummary(content.getSummary());
        readModel.setContentType(content.getContentType());
        readModel.setStatus(content.getStatus());
        readModel.setPublishedAt(content.getPublishedAt());
        readModel.setViewCount(content.getViewCount());
        readModel.setTierRequired(content.getTierRequired());

        // Featured image URL (construct from file path)
        if (content.getFeaturedImage() != null) {
            readModel.setFeaturedImageUrl(content.getFeaturedImage().getFilePath());
        }

        // Denormalize author info
        if (content.getAuthor() != null) {
            readModel.setAuthorId(content.getAuthor().getId());
            readModel.setAuthorName(content.getAuthor().getFullName());
            // Assuming User has avatar field
            // readModel.setAuthorAvatarUrl(content.getAuthor().getAvatarUrl());
        }

        // Denormalize category names
        if (content.getCategories() != null && !content.getCategories().isEmpty()) {
            String categoryNames = content.getCategories().stream()
                .map(cat -> cat.getName())
                .collect(Collectors.joining(", "));
            readModel.setCategoryNames(categoryNames);
        }

        // Denormalize tag names
        if (content.getTags() != null && !content.getTags().isEmpty()) {
            String tagNames = content.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.joining(", "));
            readModel.setTagNames(tagNames);
        }

        // SEO fields
        readModel.setSeoTitle(content.getSeoTitle());
        readModel.setSeoDescription(content.getSeoDescription());

        // Audit fields
        readModel.setCreatedAt(content.getCreatedAt());
        readModel.setUpdatedAt(content.getUpdatedAt());

        return readModelRepository.save(readModel);
    }

    /**
     * Delete read model
     */
    @Transactional
    public void deleteReadModel(UUID contentId) {
        readModelRepository.deleteById(contentId);
    }

    /**
     * Find by slug
     */
    @Transactional(readOnly = true)
    public Optional<ContentReadModel> findBySlug(String slug, String tenantId) {
        return readModelRepository.findByTenantIdAndSlug(tenantId, slug);
    }

    /**
     * Find published content
     */
    @Transactional(readOnly = true)
    public Page<ContentReadModel> findPublished(String tenantId, Pageable pageable) {
        return readModelRepository.findByTenantIdAndStatus(tenantId, ContentStatus.PUBLISHED, pageable);
    }

    /**
     * Find accessible content for user tier
     */
    @Transactional(readOnly = true)
    public Page<ContentReadModel> findAccessibleForTier(MemberTier userTier, Pageable pageable) {
        return readModelRepository.findAccessibleForTier(userTier.getLevel(), pageable);
    }

    /**
     * Find recently published
     */
    @Transactional(readOnly = true)
    public Page<ContentReadModel> findRecentlyPublished(Instant since, Pageable pageable) {
        return readModelRepository.findRecentlyPublished(since, pageable);
    }

    /**
     * Find trending
     */
    @Transactional(readOnly = true)
    public Page<ContentReadModel> findTrending(Instant since, Pageable pageable) {
        return readModelRepository.findTrending(since, pageable);
    }

    /**
     * Search content
     */
    @Transactional(readOnly = true)
    public Page<ContentReadModel> search(String keyword, Pageable pageable) {
        return readModelRepository.search(keyword, pageable);
    }

    /**
     * Count by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(ContentStatus status) {
        return readModelRepository.countByStatus(status);
    }
}
