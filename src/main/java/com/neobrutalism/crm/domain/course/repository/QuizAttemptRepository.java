package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.domain.course.model.QuizAttempt;
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
 * Repository for QuizAttempt entity
 */
@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    /**
     * Find attempts by user and quiz
     */
    List<QuizAttempt> findByUserIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(
        UUID userId,
        UUID quizId
    );

    /**
     * Find latest attempt by user and quiz
     */
    Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(
        UUID userId,
        UUID quizId
    );

    /**
     * Find attempts by enrollment
     */
    List<QuizAttempt> findByEnrollmentIdAndDeletedFalse(UUID enrollmentId);

    /**
     * Find attempts by quiz
     */
    Page<QuizAttempt> findByQuizIdAndDeletedFalse(UUID quizId, Pageable pageable);

    /**
     * Find passed attempts by user and quiz
     */
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND " +
           "qa.isPassed = true AND qa.deleted = false ORDER BY qa.score DESC")
    List<QuizAttempt> findPassedAttempts(@Param("userId") UUID userId, @Param("quizId") UUID quizId);

    /**
     * Find best attempt by user and quiz
     */
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND " +
           "qa.deleted = false ORDER BY qa.score DESC, qa.submittedAt ASC")
    Optional<QuizAttempt> findBestAttempt(@Param("userId") UUID userId, @Param("quizId") UUID quizId);

    /**
     * Count attempts by user and quiz
     */
    long countByUserIdAndQuizIdAndDeletedFalse(UUID userId, UUID quizId);

    /**
     * Count passed attempts
     */
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND " +
           "qa.isPassed = true AND qa.deleted = false")
    long countPassedAttempts(@Param("quizId") UUID quizId);

    /**
     * Get average score for quiz
     */
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND " +
           "qa.submittedAt IS NOT NULL AND qa.deleted = false")
    Optional<Double> getAverageScore(@Param("quizId") UUID quizId);

    /**
     * Find submitted attempts
     */
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND " +
           "qa.submittedAt IS NOT NULL AND qa.deleted = false")
    Page<QuizAttempt> findSubmittedAttempts(@Param("quizId") UUID quizId, Pageable pageable);

    /**
     * Find in-progress attempts by user
     */
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND " +
           "qa.submittedAt IS NULL AND qa.deleted = false")
    List<QuizAttempt> findInProgressAttempts(@Param("userId") UUID userId);

    /**
     * Check if user passed quiz
     */
    @Query("SELECT CASE WHEN COUNT(qa) > 0 THEN true ELSE false END FROM QuizAttempt qa " +
           "WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND " +
           "qa.isPassed = true AND qa.deleted = false")
    boolean hasUserPassedQuiz(@Param("userId") UUID userId, @Param("quizId") UUID quizId);
}
