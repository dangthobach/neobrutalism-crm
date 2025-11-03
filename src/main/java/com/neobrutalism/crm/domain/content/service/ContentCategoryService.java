package com.neobrutalism.crm.domain.content.service;

import com.neobrutalism.crm.common.service.SoftDeleteService;
import com.neobrutalism.crm.domain.content.dto.CategoryRequest;
import com.neobrutalism.crm.domain.content.model.ContentCategory;
import com.neobrutalism.crm.domain.content.repository.ContentCategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for ContentCategory entity
 */
@Service
@Slf4j
public class ContentCategoryService extends SoftDeleteService<ContentCategory> {

    private final ContentCategoryRepository categoryRepository;

    public ContentCategoryService(ContentCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    protected ContentCategoryRepository getRepository() {
        return categoryRepository;
    }

    @Override
    protected String getEntityName() {
        return "ContentCategory";
    }

    /**
     * Create new category
     */
    @Transactional
    public ContentCategory createCategory(CategoryRequest request, String tenantId) {
        log.info("Creating content category: {}", request.getName());

        // Check slug uniqueness
        if (categoryRepository.existsByTenantIdAndSlugAndDeletedFalse(tenantId, request.getSlug())) {
            throw new IllegalArgumentException("Category with slug '" + request.getSlug() + "' already exists");
        }

        ContentCategory category = new ContentCategory();
        category.setTenantId(tenantId);
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        // Set parent if provided
        if (request.getParentId() != null) {
            ContentCategory parent = findById(request.getParentId());
            category.setParent(parent);
        }

        ContentCategory saved = create(category);
        log.info("Category created successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Update category
     */
    @Transactional
    public ContentCategory updateCategory(UUID id, CategoryRequest request) {
        log.info("Updating category: {}", id);

        ContentCategory category = findById(id);

        if (request.getName() != null) {
            category.setName(request.getName());
        }

        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
            // Check new slug uniqueness
            if (categoryRepository.existsByTenantIdAndSlugAndDeletedFalse(category.getTenantId(), request.getSlug())) {
                throw new IllegalArgumentException("Category with slug '" + request.getSlug() + "' already exists");
            }
            category.setSlug(request.getSlug());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        if (request.getParentId() != null) {
            ContentCategory parent = findById(request.getParentId());

            // Prevent circular reference
            if (isCircularReference(category, parent)) {
                throw new IllegalArgumentException("Cannot set parent: circular reference detected");
            }

            category.setParent(parent);
        }

        ContentCategory updated = update(id, category);
        log.info("Category updated successfully: {}", id);
        return updated;
    }

    /**
     * Find category by slug
     */
    @Transactional(readOnly = true)
    public ContentCategory findBySlug(String slug, String tenantId) {
        return categoryRepository.findByTenantIdAndSlugAndDeletedFalse(tenantId, slug)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with slug: " + slug));
    }

    /**
     * Find root categories (no parent)
     */
    @Transactional(readOnly = true)
    public List<ContentCategory> findRootCategories(String tenantId) {
        return categoryRepository.findByTenantIdAndParentIsNullAndDeletedFalseOrderBySortOrderAsc(tenantId);
    }

    /**
     * Find children of a category
     */
    @Transactional(readOnly = true)
    public List<ContentCategory> findChildren(UUID parentId) {
        return categoryRepository.findByParentIdAndDeletedFalseOrderBySortOrderAsc(parentId);
    }

    /**
     * Find all categories with content count
     */
    @Transactional(readOnly = true)
    public List<Object[]> findAllWithContentCount(String tenantId) {
        return categoryRepository.findAllWithContentCount(tenantId);
    }

    /**
     * Check for circular reference in category hierarchy
     */
    private boolean isCircularReference(ContentCategory category, ContentCategory newParent) {
        if (newParent == null) {
            return false;
        }

        if (category.getId().equals(newParent.getId())) {
            return true;
        }

        ContentCategory current = newParent.getParent();
        while (current != null) {
            if (current.getId().equals(category.getId())) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    /**
     * Count categories by parent
     */
    @Transactional(readOnly = true)
    public long countByParent(UUID parentId) {
        return categoryRepository.countByParentIdAndDeletedFalse(parentId);
    }
}
