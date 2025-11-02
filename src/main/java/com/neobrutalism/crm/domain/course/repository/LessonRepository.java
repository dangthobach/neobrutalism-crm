package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.common.enums.LessonType;
import com.neobrutalism.crm.domain.course.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Lesson entity
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    /**
     * Find lessons by module
     */
    List<Lesson> findByModuleIdAndDeletedFalseOrderBySortOrderAsc(UUID moduleId);

    /**
     * Find lessons by course
     */
    @Query("SELECT l FROM Lesson l WHERE l.module.course.id = :courseId AND l.deleted = false " +
           "ORDER BY l.module.sortOrder, l.sortOrder")
    List<Lesson> findByCourseId(@Param("courseId") UUID courseId);

    /**
     * Find lessons by type
     */
    List<Lesson> findByLessonTypeAndDeletedFalse(LessonType lessonType);

    /**
     * Find lesson by module and sort order
     */
    Optional<Lesson> findByModuleIdAndSortOrderAndDeletedFalse(UUID moduleId, Integer sortOrder);

    /**
     * Count lessons by module
     */
    long countByModuleIdAndDeletedFalse(UUID moduleId);

    /**
     * Count lessons by course
     */
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = :courseId AND l.deleted = false")
    long countByCourseId(@Param("courseId") UUID courseId);

    /**
     * Get max sort order for module
     */
    @Query("SELECT MAX(l.sortOrder) FROM Lesson l WHERE l.module.id = :moduleId AND l.deleted = false")
    Optional<Integer> findMaxSortOrderByModule(@Param("moduleId") UUID moduleId);

    /**
     * Find lessons with quizzes
     */
    @Query("SELECT l FROM Lesson l LEFT JOIN FETCH l.quiz WHERE l.module.id = :moduleId AND " +
           "l.deleted = false ORDER BY l.sortOrder")
    List<Lesson> findByModuleWithQuizzes(@Param("moduleId") UUID moduleId);

    /**
     * Find video lessons
     */
    @Query("SELECT l FROM Lesson l WHERE l.lessonType = 'VIDEO' AND l.module.course.id = :courseId AND " +
           "l.deleted = false")
    List<Lesson> findVideoLessonsByCourse(@Param("courseId") UUID courseId);

    /**
     * Get total duration for course
     */
    @Query("SELECT COALESCE(SUM(l.videoDurationSeconds), 0) FROM Lesson l " +
           "WHERE l.module.course.id = :courseId AND l.deleted = false")
    Integer getTotalDurationByCourse(@Param("courseId") UUID courseId);
}
