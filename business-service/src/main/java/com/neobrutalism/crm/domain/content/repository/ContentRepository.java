package com.neobrutalism.crm.domain.content.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.content.model.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Content entity
 */
@Repository
public interface ContentRepository extends StatefulRepository<Content, ContentStatus> {

    /**
     * Find content by slug
     */
    Optional<Content> findBySlugAndDeletedFalse(String slug);

    /**
     * Find content by slug and tenant
     */
    Optional<Content> findByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Check if slug exists
     */
    boolean existsBySlugAndDeletedFalse(String slug);

    /**
     * Check if slug exists for tenant
     */
    boolean existsByTenantIdAndSlugAndDeletedFalse(String tenantId, String slug);

    /**
     * Find all published content
     */
    Page<Content> findByStatusAndDeletedFalse(ContentStatus status, Pageable pageable);

    /**
     * Find published content by type
     */
    Page<Content> findByContentTypeAndStatusAndDeletedFalse(
        ContentType contentType,
        ContentStatus status,
        Pageable pageable
    );

    /**
     * Find content by author
     */
    Page<Content> findByAuthorIdAndDeletedFalse(UUID authorId, Pageable pageable);

    /**
     * Find content by tier requirement
     */
    Page<Content> findByTierRequiredAndStatusAndDeletedFalse(
        MemberTier tierRequired,
        ContentStatus status,
        Pageable pageable
    );

    /**
     * Find content by series
     */
    @Query("SELECT c FROM Content c WHERE c.series.id = :seriesId AND c.deleted = false ORDER BY c.seriesOrder ASC")
    List<Content> findBySeriesIdOrderBySeriesOrder(@Param("seriesId") UUID seriesId);

    /**
     * Find recently published content
     */
    @Query("SELECT c FROM Content c WHERE c.status = 'PUBLISHED' AND c.deleted = false " +
           "AND c.publishedAt >= :since ORDER BY c.publishedAt DESC")
    Page<Content> findRecentlyPublished(@Param("since") Instant since, Pageable pageable);

    /**
     * Find trending content (most viewed in period)
     */
    @Query("SELECT c FROM Content c WHERE c.status = 'PUBLISHED' AND c.deleted = false " +
           "AND c.publishedAt >= :since ORDER BY c.viewCount DESC")
    Page<Content> findTrending(@Param("since") Instant since, Pageable pageable);

    /**
     * Find content by category
     */
    @Query("SELECT c FROM Content c JOIN c.categories cat WHERE cat.id = :categoryId " +
           "AND c.status = 'PUBLISHED' AND c.deleted = false ORDER BY c.publishedAt DESC")
    Page<Content> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Find content by tag
     */
    @Query("SELECT c FROM Content c JOIN c.tags tag WHERE tag.id = :tagId " +
           "AND c.status = 'PUBLISHED' AND c.deleted = false ORDER BY c.publishedAt DESC")
    Page<Content> findByTagId(@Param("tagId") UUID tagId, Pageable pageable);

    /**
     * Search content by title or body
     */
    @Query("SELECT c FROM Content c WHERE c.deleted = false AND c.status = 'PUBLISHED' " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Content> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count content by author
     */
    long countByAuthorIdAndDeletedFalse(UUID authorId);

    /**
     * Count published content
     */
    long countByStatusAndDeletedFalse(ContentStatus status);

    /**
     * Increment view count
     */
    @Modifying
    @Query("UPDATE Content c SET c.viewCount = c.viewCount + 1 WHERE c.id = :contentId")
    void incrementViewCount(@Param("contentId") UUID contentId);

    /**
     * Find content that needs to be published (scheduled publishing)
     */
    @Query("SELECT c FROM Content c WHERE c.status = 'REVIEW' AND c.deleted = false " +
           "AND c.publishedAt IS NOT NULL AND c.publishedAt <= :now")
    List<Content> findScheduledForPublishing(@Param("now") Instant now);
}
