package com.neobrutalism.crm.domain.content.repository;

import com.neobrutalism.crm.common.enums.ContentStatus;
import com.neobrutalism.crm.common.enums.ContentType;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.content.model.ContentReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ContentReadModel (CQRS Read Model)
 */
@Repository
public interface ContentReadModelRepository extends JpaRepository<ContentReadModel, UUID> {

    /**
     * Find by slug
     */
    Optional<ContentReadModel> findBySlug(String slug);

    /**
     * Find by slug and tenant
     */
    Optional<ContentReadModel> findByTenantIdAndSlug(String tenantId, String slug);

    /**
     * Find all published content
     */
    Page<ContentReadModel> findByStatus(ContentStatus status, Pageable pageable);

    /**
     * Find published content by tenant
     */
    Page<ContentReadModel> findByTenantIdAndStatus(String tenantId, ContentStatus status, Pageable pageable);

    /**
     * Find by type and status
     */
    Page<ContentReadModel> findByContentTypeAndStatus(
        ContentType contentType,
        ContentStatus status,
        Pageable pageable
    );

    /**
     * Find by tenant, type and status
     */
    Page<ContentReadModel> findByTenantIdAndContentTypeAndStatus(
        String tenantId,
        ContentType contentType,
        ContentStatus status,
        Pageable pageable
    );

    /**
     * Find by author
     */
    Page<ContentReadModel> findByAuthorId(UUID authorId, Pageable pageable);

    /**
     * Find by tier requirement
     */
    Page<ContentReadModel> findByTierRequiredAndStatus(
        MemberTier tierRequired,
        ContentStatus status,
        Pageable pageable
    );

    /**
     * Find accessible content for user tier
     */
    @Query("SELECT c FROM ContentReadModel c WHERE c.status = 'PUBLISHED' " +
           "AND c.tierRequired <= :userTierLevel " +
           "ORDER BY c.publishedAt DESC")
    Page<ContentReadModel> findAccessibleForTier(
        @Param("userTierLevel") int userTierLevel,
        Pageable pageable
    );

    /**
     * Find recently published
     */
    @Query("SELECT c FROM ContentReadModel c WHERE c.status = 'PUBLISHED' " +
           "AND c.publishedAt >= :since ORDER BY c.publishedAt DESC")
    Page<ContentReadModel> findRecentlyPublished(@Param("since") Instant since, Pageable pageable);

    /**
     * Find trending (most viewed)
     */
    @Query("SELECT c FROM ContentReadModel c WHERE c.status = 'PUBLISHED' " +
           "AND c.publishedAt >= :since ORDER BY c.viewCount DESC")
    Page<ContentReadModel> findTrending(@Param("since") Instant since, Pageable pageable);

    /**
     * Search by title or description
     */
    @Query("SELECT c FROM ContentReadModel c WHERE c.status = 'PUBLISHED' " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ContentReadModel> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count by status
     */
    long countByStatus(ContentStatus status);

    /**
     * Count by author
     */
    long countByAuthorId(UUID authorId);
}
