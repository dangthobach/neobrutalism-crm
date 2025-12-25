package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.common.enums.QuestionType;
import com.neobrutalism.crm.domain.course.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for QuizQuestion entity
 */
@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    /**
     * Find questions by quiz
     */
    List<QuizQuestion> findByQuizIdAndDeletedFalseOrderBySortOrderAsc(UUID quizId);

    /**
     * Find questions by type
     */
    List<QuizQuestion> findByQuestionTypeAndDeletedFalse(QuestionType questionType);

    /**
     * Count questions by quiz
     */
    long countByQuizIdAndDeletedFalse(UUID quizId);

    /**
     * Get max sort order for quiz
     */
    @Query("SELECT MAX(q.sortOrder) FROM QuizQuestion q WHERE q.quiz.id = :quizId AND q.deleted = false")
    Integer findMaxSortOrderByQuiz(@Param("quizId") UUID quizId);

    /**
     * Get total points for quiz
     */
    @Query("SELECT COALESCE(SUM(q.points), 0) FROM QuizQuestion q WHERE q.quiz.id = :quizId AND q.deleted = false")
    Integer getTotalPointsByQuiz(@Param("quizId") UUID quizId);
}
