package com.neobrutalism.crm.domain.content.repository;

import com.neobrutalism.crm.domain.content.model.ContentView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ContentView entity
 */
@Repository
public interface ContentViewRepository extends JpaRepository<ContentView, UUID> {

    /**
     * Find views by content
     */
    Page<ContentView> findByContentId(UUID contentId, Pageable pageable);

    /**
     * Find views by user
     */
    Page<ContentView> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find views by session
     */
    List<ContentView> findBySessionId(String sessionId);

    /**
     * Count views by content
     */
    long countByContentId(UUID contentId);

    /**
     * Count unique users who viewed content
     */
    @Query("SELECT COUNT(DISTINCT cv.user.id) FROM ContentView cv WHERE cv.content.id = :contentId AND cv.user IS NOT NULL")
    long countUniqueUsersByContentId(@Param("contentId") UUID contentId);

    /**
     * Count views by user
     */
    long countByUserId(UUID userId);

    /**
     * Find views in time range
     */
    @Query("SELECT cv FROM ContentView cv WHERE cv.viewedAt >= :start AND cv.viewedAt <= :end ORDER BY cv.viewedAt DESC")
    Page<ContentView> findByViewedAtBetween(
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );

    /**
     * Find recent views by content
     */
    @Query("SELECT cv FROM ContentView cv WHERE cv.content.id = :contentId AND cv.viewedAt >= :since ORDER BY cv.viewedAt DESC")
    List<ContentView> findRecentByContentId(
        @Param("contentId") UUID contentId,
        @Param("since") Instant since
    );

    /**
     * Check if user has viewed content
     */
    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

    /**
     * Get total time spent by user on content
     */
    @Query("SELECT COALESCE(SUM(cv.timeSpentSeconds), 0) FROM ContentView cv WHERE cv.content.id = :contentId AND cv.user.id = :userId")
    int getTotalTimeSpentByUser(
        @Param("contentId") UUID contentId,
        @Param("userId") UUID userId
    );

    /**
     * Find most viewed content in time period
     */
    @Query("SELECT cv.content.id, COUNT(cv) as viewCount FROM ContentView cv " +
           "WHERE cv.viewedAt >= :since " +
           "GROUP BY cv.content.id ORDER BY viewCount DESC")
    List<Object[]> findMostViewedContent(
        @Param("since") Instant since,
        Pageable pageable
    );

    /**
     * Get user's viewing history
     */
    @Query("SELECT cv FROM ContentView cv WHERE cv.user.id = :userId ORDER BY cv.viewedAt DESC")
    Page<ContentView> findUserViewHistory(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get average time spent on content
     */
    @Query("SELECT AVG(cv.timeSpentSeconds) FROM ContentView cv WHERE cv.content.id = :contentId AND cv.timeSpentSeconds > 0")
    Double getAverageTimeSpent(@Param("contentId") UUID contentId);

    /**
     * Get average scroll percentage
     */
    @Query("SELECT AVG(cv.scrollPercentage) FROM ContentView cv WHERE cv.content.id = :contentId AND cv.scrollPercentage > 0")
    Double getAverageScrollPercentage(@Param("contentId") UUID contentId);
}
