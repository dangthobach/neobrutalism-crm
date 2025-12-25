package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.domain.course.model.CourseReview;
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
 * Repository for CourseReview entity
 */
@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    /**
     * Find review by user and course
     */
    Optional<CourseReview> findByUserIdAndCourseIdAndDeletedFalse(UUID userId, UUID courseId);

    /**
     * Find reviews by course
     */
    Page<CourseReview> findByCourseIdAndDeletedFalse(UUID courseId, Pageable pageable);

    /**
     * Find reviews by user
     */
    Page<CourseReview> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    /**
     * Find featured reviews
     */
    Page<CourseReview> findByIsFeaturedTrueAndDeletedFalse(Pageable pageable);

    /**
     * Find featured reviews by course
     */
    List<CourseReview> findByCourseIdAndIsFeaturedTrueAndDeletedFalse(UUID courseId);

    /**
     * Find verified purchase reviews
     */
    Page<CourseReview> findByCourseIdAndIsVerifiedPurchaseTrueAndDeletedFalse(
        UUID courseId,
        Pageable pageable
    );

    /**
     * Find reviews by rating
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.id = :courseId AND cr.rating = :rating AND " +
           "cr.deleted = false")
    Page<CourseReview> findByCourseAndRating(
        @Param("courseId") UUID courseId,
        @Param("rating") Integer rating,
        Pageable pageable
    );

    /**
     * Find reviews by rating range
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.id = :courseId AND " +
           "cr.rating BETWEEN :minRating AND :maxRating AND cr.deleted = false")
    Page<CourseReview> findByCourseAndRatingRange(
        @Param("courseId") UUID courseId,
        @Param("minRating") Integer minRating,
        @Param("maxRating") Integer maxRating,
        Pageable pageable
    );

    /**
     * Find reviews with instructor responses
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.id = :courseId AND " +
           "cr.instructorResponse IS NOT NULL AND cr.deleted = false")
    Page<CourseReview> findWithInstructorResponse(
        @Param("courseId") UUID courseId,
        Pageable pageable
    );

    /**
     * Find reviews without instructor responses
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.id = :courseId AND " +
           "cr.instructorResponse IS NULL AND cr.deleted = false")
    Page<CourseReview> findWithoutInstructorResponse(
        @Param("courseId") UUID courseId,
        Pageable pageable
    );

    /**
     * Count reviews by course
     */
    long countByCourseIdAndDeletedFalse(UUID courseId);

    /**
     * Count reviews by rating
     */
    long countByCourseIdAndRatingAndDeletedFalse(UUID courseId, Integer rating);

    /**
     * Get average rating for course
     */
    @Query("SELECT AVG(cr.rating) FROM CourseReview cr WHERE cr.course.id = :courseId AND cr.deleted = false")
    Optional<Double> getAverageRating(@Param("courseId") UUID courseId);

    /**
     * Check if user has reviewed course
     */
    boolean existsByUserIdAndCourseIdAndDeletedFalse(UUID userId, UUID courseId);

    /**
     * Find most helpful reviews
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.id = :courseId AND cr.deleted = false " +
           "ORDER BY cr.helpfulCount DESC")
    Page<CourseReview> findMostHelpful(@Param("courseId") UUID courseId, Pageable pageable);

    /**
     * Find recent reviews
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.id = :courseId AND cr.deleted = false " +
           "ORDER BY cr.reviewedAt DESC")
    Page<CourseReview> findRecent(@Param("courseId") UUID courseId, Pageable pageable);

    /**
     * Find reported reviews
     */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.reportedCount > 0 AND cr.deleted = false " +
           "ORDER BY cr.reportedCount DESC")
    Page<CourseReview> findReported(Pageable pageable);
}
