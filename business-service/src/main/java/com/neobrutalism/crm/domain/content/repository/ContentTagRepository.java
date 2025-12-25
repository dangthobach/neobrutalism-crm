package com.neobrutalism.crm.domain.content.repository;

import com.neobrutalism.crm.common.repository.SoftDeleteRepository;
import com.neobrutalism.crm.domain.content.model.ContentTag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ContentTag entity
 */
@Repository
public interface ContentTagRepository extends SoftDeleteRepository<ContentTag> {

    /**
     * Find tag by slug
     */
    Optional<ContentTag> findBySlugAndDeletedFalse(String slug);

    /**
     * Find tag by slug and tenant
     */
    Optional<ContentTag> findByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Find tag by name
     */
    Optional<ContentTag> findByNameAndDeletedFalse(String name);

    /**
     * Find tag by name and tenant
     */
    Optional<ContentTag> findByTenantIdAndNameAndDeletedFalse(String tenantId, String name);

    /**
     * Check if slug exists
     */
    boolean existsBySlugAndDeletedFalse(String slug);

    /**
     * Check if slug exists for tenant
     */
    boolean existsByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Search tags by name
     */
    List<ContentTag> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

    /**
     * Find all tags by tenant
     */
    List<ContentTag> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Find popular tags (most used)
     */
    @Query("SELECT t, COUNT(ct) as contentCount FROM ContentTag t " +
           "LEFT JOIN t.contents ct " +
           "WHERE t.deleted = false AND t.tenantId = :tenantId " +
           "GROUP BY t ORDER BY contentCount DESC")
    List<Object[]> findPopularTags(@Param("tenantId") String tenantId);

    /**
     * Find tags with content count
     */
    @Query("SELECT t, COUNT(ct) as contentCount FROM ContentTag t " +
           "LEFT JOIN t.contents ct " +
           "WHERE t.deleted = false AND t.tenantId = :tenantId " +
           "GROUP BY t ORDER BY t.name ASC")
    List<Object[]> findAllWithContentCount(@Param("tenantId") String tenantId);
}
