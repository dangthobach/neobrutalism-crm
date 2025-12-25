package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.common.enums.AchievementType;
import com.neobrutalism.crm.domain.course.model.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Achievement entity
 */
@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    /**
     * Find by code
     */
    Optional<Achievement> findByCodeAndDeletedFalse(String code);

    /**
     * Find by type
     */
    List<Achievement> findByAchievementTypeAndDeletedFalse(AchievementType type);

    /**
     * Find global achievements
     */
    @Query("SELECT a FROM Achievement a WHERE a.course IS NULL AND a.deleted = false " +
           "ORDER BY a.displayOrder")
    List<Achievement> findGlobalAchievements();

    /**
     * Find course-specific achievements
     */
    @Query("SELECT a FROM Achievement a WHERE a.course.id = :courseId AND a.deleted = false " +
           "ORDER BY a.displayOrder")
    List<Achievement> findByCourse(@Param("courseId") UUID courseId);

    /**
     * Find visible achievements
     */
    @Query("SELECT a FROM Achievement a WHERE a.isHidden = false AND a.deleted = false " +
           "ORDER BY a.displayOrder")
    Page<Achievement> findVisible(Pageable pageable);

    /**
     * Find hidden achievements
     */
    @Query("SELECT a FROM Achievement a WHERE a.isHidden = true AND a.deleted = false " +
           "ORDER BY a.displayOrder")
    List<Achievement> findHidden();

    /**
     * Check if code exists
     */
    boolean existsByCodeAndDeletedFalse(String code);

    /**
     * Count achievements by type
     */
    long countByAchievementTypeAndDeletedFalse(AchievementType type);

    /**
     * Find achievements by point range
     */
    @Query("SELECT a FROM Achievement a WHERE a.points BETWEEN :minPoints AND :maxPoints AND " +
           "a.deleted = false ORDER BY a.points DESC")
    List<Achievement> findByPointRange(
        @Param("minPoints") Integer minPoints,
        @Param("maxPoints") Integer maxPoints
    );
}
