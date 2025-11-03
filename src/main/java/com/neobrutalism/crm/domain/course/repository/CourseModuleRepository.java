package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.domain.course.model.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CourseModule entity
 */
@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    /**
     * Find modules by course
     */
    List<CourseModule> findByCourseIdAndDeletedFalseOrderBySortOrderAsc(UUID courseId);

    /**
     * Find module by course and sort order
     */
    Optional<CourseModule> findByCourseIdAndSortOrderAndDeletedFalse(UUID courseId, Integer sortOrder);

    /**
     * Count modules by course
     */
    long countByCourseIdAndDeletedFalse(UUID courseId);

    /**
     * Get max sort order for course
     */
    @Query("SELECT MAX(m.sortOrder) FROM CourseModule m WHERE m.course.id = :courseId AND m.deleted = false")
    Optional<Integer> findMaxSortOrderByCourse(@Param("courseId") UUID courseId);

    /**
     * Find modules with lessons
     */
    @Query("SELECT DISTINCT m FROM CourseModule m " +
           "LEFT JOIN FETCH m.lessons l " +
           "WHERE m.course.id = :courseId AND m.deleted = false " +
           "ORDER BY m.sortOrder ASC")
    List<CourseModule> findByCourseWithLessons(@Param("courseId") UUID courseId);
}
