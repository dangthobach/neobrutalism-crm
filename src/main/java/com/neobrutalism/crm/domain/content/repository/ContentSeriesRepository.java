package com.neobrutalism.crm.domain.content.repository;

import com.neobrutalism.crm.common.repository.SoftDeleteRepository;
import com.neobrutalism.crm.domain.content.model.ContentSeries;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ContentSeries entity
 */
@Repository
public interface ContentSeriesRepository extends SoftDeleteRepository<ContentSeries> {

    /**
     * Find series by slug
     */
    Optional<ContentSeries> findBySlugAndDeletedFalse(String slug);

    /**
     * Find series by slug and tenant
     */
    Optional<ContentSeries> findByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Check if slug exists
     */
    boolean existsBySlugAndDeletedFalse(String slug);

    /**
     * Check if slug exists for tenant
     */
    boolean existsByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Find all series ordered by sort order
     */
    List<ContentSeries> findByDeletedFalseOrderBySortOrderAsc();

    /**
     * Find all series by tenant ordered by sort order
     */
    List<ContentSeries> findByTenantIdAndDeletedFalseOrderBySortOrderAsc(String tenantId);

    /**
     * Find series with content count
     */
    @Query("SELECT s, COUNT(c) as contentCount FROM ContentSeries s " +
           "LEFT JOIN s.contents c " +
           "WHERE s.deleted = false AND s.tenantId = :tenantId " +
           "GROUP BY s ORDER BY s.sortOrder ASC")
    List<Object[]> findAllWithContentCount(@Param("tenantId") String tenantId);
}
