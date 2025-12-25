package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.common.enums.CourseLevel;
import com.neobrutalism.crm.common.enums.CourseStatus;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Course entity
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    /**
     * Find by code
     */
    Optional<Course> findByCodeAndDeletedFalse(String code);

    /**
     * Find by slug
     */
    Optional<Course> findBySlugAndDeletedFalse(String slug);

    /**
     * Find all published courses
     */
    Page<Course> findByStatusAndDeletedFalse(CourseStatus status, Pageable pageable);

    /**
     * Find courses by instructor
     */
    Page<Course> findByInstructorIdAndDeletedFalse(UUID instructorId, Pageable pageable);

    /**
     * Find courses by tier
     */
    Page<Course> findByTierRequiredAndStatusAndDeletedFalse(
        MemberTier tier,
        CourseStatus status,
        Pageable pageable
    );

    /**
     * Find courses by level
     */
    Page<Course> findByCourseLevelAndStatusAndDeletedFalse(
        CourseLevel level,
        CourseStatus status,
        Pageable pageable
    );

    /**
     * Find courses by category
     */
    @Query("SELECT c FROM Course c WHERE c.category.id = :categoryId AND c.status = :status AND c.deleted = false")
    Page<Course> findByCategoryAndStatus(
        @Param("categoryId") UUID categoryId,
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    /**
     * Search courses by title or description
     */
    @Query("SELECT c FROM Course c WHERE " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "c.status = :status AND c.deleted = false")
    Page<Course> searchCourses(
        @Param("keyword") String keyword,
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    /**
     * Find free courses
     */
    @Query("SELECT c FROM Course c WHERE c.price = 0 AND c.status = :status AND c.deleted = false")
    Page<Course> findFreeCourses(@Param("status") CourseStatus status, Pageable pageable);

    /**
     * Find paid courses
     */
    @Query("SELECT c FROM Course c WHERE c.price > 0 AND c.status = :status AND c.deleted = false")
    Page<Course> findPaidCourses(@Param("status") CourseStatus status, Pageable pageable);

    /**
     * Find courses by price range
     */
    @Query("SELECT c FROM Course c WHERE c.price BETWEEN :minPrice AND :maxPrice AND " +
           "c.status = :status AND c.deleted = false")
    Page<Course> findByPriceRange(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    /**
     * Find top rated courses
     */
    @Query("SELECT c FROM Course c WHERE c.ratingAverage >= :minRating AND " +
           "c.status = :status AND c.deleted = false ORDER BY c.ratingAverage DESC")
    Page<Course> findTopRatedCourses(
        @Param("minRating") BigDecimal minRating,
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    /**
     * Find popular courses (by enrollment count)
     */
    @Query("SELECT c FROM Course c WHERE c.status = :status AND c.deleted = false " +
           "ORDER BY c.enrollmentCount DESC")
    Page<Course> findPopularCourses(@Param("status") CourseStatus status, Pageable pageable);

    /**
     * Find recently published courses
     */
    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' AND c.publishedAt IS NOT NULL AND " +
           "c.deleted = false ORDER BY c.publishedAt DESC")
    Page<Course> findRecentlyPublished(Pageable pageable);

    /**
     * Count courses by instructor
     */
    long countByInstructorIdAndDeletedFalse(UUID instructorId);

    /**
     * Count published courses
     */
    long countByStatusAndDeletedFalse(CourseStatus status);

    /**
     * Find courses by tier accessible to user
     */
    @Query("SELECT c FROM Course c WHERE c.tierRequired IN :tiers AND " +
           "c.status = :status AND c.deleted = false")
    Page<Course> findByAccessibleTiers(
        @Param("tiers") List<MemberTier> tiers,
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    /**
     * Check if code exists
     */
    boolean existsByCodeAndDeletedFalse(String code);

    /**
     * Check if slug exists
     */
    boolean existsBySlugAndDeletedFalse(String slug);
}
