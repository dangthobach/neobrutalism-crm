package com.neobrutalism.crm.domain.content.service;

import com.neobrutalism.crm.common.service.SoftDeleteService;
import com.neobrutalism.crm.domain.content.dto.TagRequest;
import com.neobrutalism.crm.domain.content.model.ContentTag;
import com.neobrutalism.crm.domain.content.repository.ContentTagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for ContentTag entity
 */
@Service
@Slf4j
public class ContentTagService extends SoftDeleteService<ContentTag> {

    private final ContentTagRepository tagRepository;

    public ContentTagService(ContentTagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    protected ContentTagRepository getRepository() {
        return tagRepository;
    }

    @Override
    protected String getEntityName() {
        return "ContentTag";
    }

    /**
     * Create new tag
     */
    @Transactional
    public ContentTag createTag(TagRequest request, String tenantId) {
        log.info("Creating content tag: {}", request.getName());

        // Check slug uniqueness
        if (tagRepository.existsByTenantIdAndSlugAndDeletedFalse(tenantId, request.getSlug())) {
            throw new IllegalArgumentException("Tag with slug '" + request.getSlug() + "' already exists");
        }

        ContentTag tag = new ContentTag();
        tag.setTenantId(tenantId);
        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        tag.setColor(request.getColor());

        ContentTag saved = create(tag);
        log.info("Tag created successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Update tag
     */
    @Transactional
    public ContentTag updateTag(UUID id, TagRequest request) {
        log.info("Updating tag: {}", id);

        ContentTag tag = findById(id);

        if (request.getName() != null) {
            tag.setName(request.getName());
        }

        if (request.getSlug() != null && !request.getSlug().equals(tag.getSlug())) {
            // Check new slug uniqueness
            if (tagRepository.existsByTenantIdAndSlugAndDeletedFalse(tag.getTenantId(), request.getSlug())) {
                throw new IllegalArgumentException("Tag with slug '" + request.getSlug() + "' already exists");
            }
            tag.setSlug(request.getSlug());
        }

        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }

        ContentTag updated = update(id, tag);
        log.info("Tag updated successfully: {}", id);
        return updated;
    }

    /**
     * Find tag by slug
     */
    @Transactional(readOnly = true)
    public ContentTag findBySlug(String slug, String tenantId) {
        return tagRepository.findByTenantIdAndSlugAndDeletedFalse(tenantId, slug)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found with slug: " + slug));
    }

    /**
     * Find tag by name
     */
    @Transactional(readOnly = true)
    public ContentTag findByName(String name, String tenantId) {
        return tagRepository.findByTenantIdAndNameAndDeletedFalse(tenantId, name)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found with name: " + name));
    }

    /**
     * Find or create tag by name (useful for auto-tagging)
     */
    @Transactional
    public ContentTag findOrCreateByName(String name, String tenantId) {
        return tagRepository.findByTenantIdAndNameAndDeletedFalse(tenantId, name)
            .orElseGet(() -> {
                TagRequest request = new TagRequest();
                request.setName(name);
                request.setSlug(name.toLowerCase().replaceAll("\\s+", "-"));
                return createTag(request, tenantId);
            });
    }

    /**
     * Search tags by name
     */
    @Transactional(readOnly = true)
    public List<ContentTag> searchByName(String name) {
        return tagRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
    }

    /**
     * Find all tags by tenant
     */
    @Transactional(readOnly = true)
    public List<ContentTag> findAllByTenant(String tenantId) {
        return tagRepository.findByTenantIdAndDeletedFalse(tenantId);
    }

    /**
     * Find popular tags (most used)
     */
    @Transactional(readOnly = true)
    public List<Object[]> findPopularTags(String tenantId) {
        return tagRepository.findPopularTags(tenantId);
    }

    /**
     * Find all tags with content count
     */
    @Transactional(readOnly = true)
    public List<Object[]> findAllWithContentCount(String tenantId) {
        return tagRepository.findAllWithContentCount(tenantId);
    }
}
