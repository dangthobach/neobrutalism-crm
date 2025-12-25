package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.domain.course.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Quiz entity
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    /**
     * Find quiz by lesson
     */
    Optional<Quiz> findByLessonIdAndDeletedFalse(UUID lessonId);

    /**
     * Find quizzes by course
     */
    @Query("SELECT q FROM Quiz q WHERE q.lesson.module.course.id = :courseId AND q.deleted = false")
    List<Quiz> findByCourseId(@Param("courseId") UUID courseId);

    /**
     * Find quizzes with questions
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :quizId AND q.deleted = false")
    Optional<Quiz> findByIdWithQuestions(@Param("quizId") UUID quizId);

    /**
     * Count quizzes by course
     */
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.lesson.module.course.id = :courseId AND q.deleted = false")
    long countByCourseId(@Param("courseId") UUID courseId);
}
