package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.common.enums.LessonProgressStatus;
import com.neobrutalism.crm.domain.course.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LessonProgress entity
 */
@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    /**
     * Find progress by user and lesson
     */
    Optional<LessonProgress> findByUserIdAndLessonIdAndDeletedFalse(UUID userId, UUID lessonId);

    /**
     * Find progress by enrollment
     */
    List<LessonProgress> findByEnrollmentIdAndDeletedFalse(UUID enrollmentId);

    /**
     * Find progress by user and course
     */
    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user.id = :userId AND " +
           "lp.lesson.module.course.id = :courseId AND lp.deleted = false")
    List<LessonProgress> findByUserAndCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    /**
     * Find completed lessons by user and course
     */
    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user.id = :userId AND " +
           "lp.lesson.module.course.id = :courseId AND lp.status = 'COMPLETED' AND lp.deleted = false")
    List<LessonProgress> findCompletedByUserAndCourse(
        @Param("userId") UUID userId,
        @Param("courseId") UUID courseId
    );

    /**
     * Count completed lessons by enrollment
     */
    long countByEnrollmentIdAndStatusAndDeletedFalse(UUID enrollmentId, LessonProgressStatus status);

    /**
     * Count total lessons by enrollment
     */
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = " +
           "(SELECT e.course.id FROM Enrollment e WHERE e.id = :enrollmentId) AND l.deleted = false")
    long countTotalLessonsByEnrollment(@Param("enrollmentId") UUID enrollmentId);

    /**
     * Get completion percentage for enrollment
     */
    @Query("SELECT CAST(COUNT(lp) * 100.0 / " +
           "(SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = e.course.id AND l.deleted = false) AS int) " +
           "FROM LessonProgress lp " +
           "JOIN lp.enrollment e " +
           "WHERE e.id = :enrollmentId AND lp.status = 'COMPLETED' AND lp.deleted = false")
    Optional<Integer> calculateCompletionPercentage(@Param("enrollmentId") UUID enrollmentId);

    /**
     * Get total time spent by user on course
     */
    @Query("SELECT COALESCE(SUM(lp.timeSpentSeconds), 0) FROM LessonProgress lp " +
           "WHERE lp.user.id = :userId AND lp.lesson.module.course.id = :courseId AND lp.deleted = false")
    Integer getTotalTimeSpent(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    /**
     * Find in-progress lessons by user
     */
    List<LessonProgress> findByUserIdAndStatusAndDeletedFalse(UUID userId, LessonProgressStatus status);

    /**
     * Check if lesson is completed by user
     */
    @Query("SELECT CASE WHEN COUNT(lp) > 0 THEN true ELSE false END FROM LessonProgress lp " +
           "WHERE lp.user.id = :userId AND lp.lesson.id = :lessonId AND " +
           "lp.status = 'COMPLETED' AND lp.deleted = false")
    boolean isLessonCompleted(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId);
}
