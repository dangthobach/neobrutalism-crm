package com.neobrutalism.crm.domain.content.repository;

import com.neobrutalism.crm.common.repository.SoftDeleteRepository;
import com.neobrutalism.crm.domain.content.model.ContentCategory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ContentCategory entity
 */
@Repository
public interface ContentCategoryRepository extends SoftDeleteRepository<ContentCategory> {

    /**
     * Find category by slug
     */
    Optional<ContentCategory> findBySlugAndDeletedFalse(String slug);

    /**
     * Find category by slug and tenant
     */
    Optional<ContentCategory> findByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Check if slug exists
     */
    boolean existsBySlugAndDeletedFalse(String slug);

    /**
     * Check if slug exists for tenant
     */
    boolean existsByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Find all root categories (no parent)
     */
    List<ContentCategory> findByParentIsNullAndDeletedFalseOrderBySortOrderAsc();

    /**
     * Find all root categories by tenant
     */
    List<ContentCategory> findByTenantIdAndParentIsNullAndDeletedFalseOrderBySortOrderAsc(String tenantId);

    /**
     * Find children of a category
     */
    List<ContentCategory> findByParentIdAndDeletedFalseOrderBySortOrderAsc(UUID parentId);

    /**
     * Count categories by parent
     */
    long countByParentIdAndDeletedFalse(UUID parentId);

    /**
     * Find all categories with their content count
     */
    @Query("SELECT c, COUNT(cc) as contentCount FROM ContentCategory c " +
           "LEFT JOIN c.contents cc " +
           "WHERE c.deleted = false AND c.tenantId = :tenantId " +
           "GROUP BY c ORDER BY c.sortOrder ASC")
    List<Object[]> findAllWithContentCount(@Param("tenantId") String tenantId);
}
