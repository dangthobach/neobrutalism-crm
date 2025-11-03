package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.domain.course.model.UserAchievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserAchievement entity
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    /**
     * Find by user and achievement
     */
    Optional<UserAchievement> findByUserIdAndAchievementIdAndDeletedFalse(
        UUID userId,
        UUID achievementId
    );

    /**
     * Find achievements by user
     */
    Page<UserAchievement> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    /**
     * Find displayed achievements by user
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND " +
           "ua.isDisplayed = true AND ua.deleted = false ORDER BY ua.earnedAt DESC")
    List<UserAchievement> findDisplayedByUser(@Param("userId") UUID userId);

    /**
     * Find recent achievements by user
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.deleted = false " +
           "ORDER BY ua.earnedAt DESC")
    Page<UserAchievement> findRecentByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find unnotified achievements
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND " +
           "ua.isNotified = false AND ua.deleted = false")
    List<UserAchievement> findUnnotifiedByUser(@Param("userId") UUID userId);

    /**
     * Count achievements by user
     */
    long countByUserIdAndDeletedFalse(UUID userId);

    /**
     * Count achievements by user and type
     */
    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.user.id = :userId AND " +
           "ua.achievement.achievementType = :type AND ua.deleted = false")
    long countByUserAndType(
        @Param("userId") UUID userId,
        @Param("type") com.neobrutalism.crm.common.enums.AchievementType type
    );

    /**
     * Get total points by user
     */
    @Query("SELECT COALESCE(SUM(ua.achievement.points), 0) FROM UserAchievement ua " +
           "WHERE ua.user.id = :userId AND ua.deleted = false")
    Integer getTotalPointsByUser(@Param("userId") UUID userId);

    /**
     * Check if user has achievement
     */
    boolean existsByUserIdAndAchievementIdAndDeletedFalse(UUID userId, UUID achievementId);

    /**
     * Find achievements earned in date range
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND " +
           "ua.earnedAt BETWEEN :startDate AND :endDate AND ua.deleted = false " +
           "ORDER BY ua.earnedAt DESC")
    List<UserAchievement> findByUserAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find achievements by completion status
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND " +
           "ua.progress >= :minProgress AND ua.deleted = false")
    List<UserAchievement> findByUserAndProgress(
        @Param("userId") UUID userId,
        @Param("minProgress") Integer minProgress
    );

    /**
     * Find users by achievement
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.achievement.id = :achievementId AND " +
           "ua.deleted = false ORDER BY ua.earnedAt DESC")
    Page<UserAchievement> findByAchievement(
        @Param("achievementId") UUID achievementId,
        Pageable pageable
    );

    /**
     * Count users who earned achievement
     */
    long countByAchievementIdAndDeletedFalse(UUID achievementId);
}
