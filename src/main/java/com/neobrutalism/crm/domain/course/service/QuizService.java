package com.neobrutalism.crm.domain.course.service;

import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.course.event.QuizCompletedEvent;
import com.neobrutalism.crm.domain.course.model.*;
import com.neobrutalism.crm.domain.course.repository.*;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for quiz management and grading
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Start a new quiz attempt
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @param enrollmentId The enrollment ID
     * @return The created quiz attempt
     */
    @Transactional
    public QuizAttempt startQuizAttempt(UUID userId, UUID quizId, UUID enrollmentId) {
        log.info("Starting quiz attempt - User: {}, Quiz: {}", userId, quizId);

        // Validate entities
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        // Check if enrollment is active
        if (!enrollment.isActive()) {
            throw new IllegalStateException("Enrollment is not active");
        }

        // Check max attempts
        if (quiz.hasMaxAttempts()) {
            long attemptCount = quizAttemptRepository.countByUserIdAndQuizIdAndDeletedFalse(userId, quizId);
            if (attemptCount >= quiz.getMaxAttempts()) {
                throw new IllegalStateException(
                    String.format("Maximum attempts (%d) reached for this quiz", quiz.getMaxAttempts())
                );
            }
        }

        // Check if user has an in-progress attempt
        List<QuizAttempt> inProgressAttempts = quizAttemptRepository.findInProgressAttempts(userId);
        Optional<QuizAttempt> existingAttempt = inProgressAttempts.stream()
            .filter(attempt -> attempt.getQuiz().getId().equals(quizId))
            .findFirst();

        if (existingAttempt.isPresent()) {
            log.warn("User {} already has an in-progress attempt for quiz {}", userId, quizId);
            return existingAttempt.get();
        }

        // Get next attempt number
        int attemptNumber = getNextAttemptNumber(userId, quizId);

        // Create new attempt
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setEnrollment(enrollment);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTotalPoints(quiz.getTotalPoints());

        attempt = quizAttemptRepository.save(attempt);
        log.info("Quiz attempt started: {} (Attempt #{})", attempt.getId(), attemptNumber);

        return attempt;
    }

    /**
     * Submit an answer for a question
     *
     * @param attemptId The attempt ID
     * @param questionId The question ID
     * @param answer The answer
     * @return Updated quiz attempt
     */
    @Transactional
    public QuizAttempt submitAnswer(UUID attemptId, UUID questionId, String answer) {
        log.debug("Submitting answer for attempt {} question {}", attemptId, questionId);

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found: " + attemptId));

        // Check if quiz is still in progress
        if (attempt.isSubmitted()) {
            throw new IllegalStateException("Quiz attempt already submitted");
        }

        // Check time limit
        if (attempt.isTimeLimitExceeded(attempt.getQuiz().getTimeLimitMinutes())) {
            log.warn("Time limit exceeded for attempt {}", attemptId);
            // Auto-submit the quiz
            return submitQuiz(attemptId);
        }

        // Add answer
        attempt.addAnswer(questionId.toString(), answer);

        return quizAttemptRepository.save(attempt);
    }

    /**
     * Submit quiz and auto-grade
     *
     * @param attemptId The attempt ID
     * @return Graded quiz attempt
     */
    @Transactional
    public QuizAttempt submitQuiz(UUID attemptId) {
        log.info("Submitting and grading quiz attempt: {}", attemptId);

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found: " + attemptId));

        // Check if already submitted
        if (attempt.isSubmitted()) {
            log.warn("Quiz attempt {} already submitted", attemptId);
            return attempt;
        }

        // Submit the attempt
        attempt.submit();

        // Auto-grade the quiz
        gradeAttempt(attempt);

        // Save graded attempt
        attempt = quizAttemptRepository.save(attempt);

        // Fire QuizCompletedEvent
        fireQuizCompletedEvent(attempt);

        log.info("Quiz submitted and graded - Score: {}, Passed: {}", attempt.getScore(), attempt.isPassed());

        return attempt;
    }

    /**
     * Grade a quiz attempt
     *
     * @param attempt The quiz attempt
     */
    private void gradeAttempt(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        List<QuizQuestion> questions = quiz.getQuestions();

        int pointsEarned = 0;
        int totalPoints = quiz.getTotalPoints();

        for (QuizQuestion question : questions) {
            String studentAnswer = attempt.getAnswer(question.getId().toString());

            if (studentAnswer != null && !studentAnswer.trim().isEmpty()) {
                boolean isCorrect = false;

                switch (question.getQuestionType()) {
                    case MULTIPLE_CHOICE:
                    case TRUE_FALSE:
                        isCorrect = question.isCorrectAnswer(studentAnswer);
                        break;

                    case SHORT_ANSWER:
                        // Case-insensitive comparison for short answers
                        isCorrect = question.isCorrectAnswer(studentAnswer);
                        break;

                    case ESSAY:
                        // Essay questions require manual grading
                        // For now, we don't award points automatically
                        log.info("Essay question {} requires manual grading", question.getId());
                        break;
                }

                if (isCorrect) {
                    pointsEarned += question.getPoints();
                }
            }
        }

        // Grade the attempt
        attempt.grade(pointsEarned, totalPoints, quiz.getPassingScore());
    }

    /**
     * Get quiz attempts for a user and quiz
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @return List of quiz attempts
     */
    @Transactional(readOnly = true)
    public List<QuizAttempt> getQuizAttempts(UUID userId, UUID quizId) {
        return quizAttemptRepository.findByUserIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(userId, quizId);
    }

    /**
     * Get latest quiz attempt
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @return Latest quiz attempt
     */
    @Transactional(readOnly = true)
    public Optional<QuizAttempt> getLatestAttempt(UUID userId, UUID quizId) {
        return quizAttemptRepository.findFirstByUserIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(userId, quizId);
    }

    /**
     * Get best quiz attempt (highest score)
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @return Best quiz attempt
     */
    @Transactional(readOnly = true)
    public Optional<QuizAttempt> getBestAttempt(UUID userId, UUID quizId) {
        return quizAttemptRepository.findBestAttempt(userId, quizId);
    }

    /**
     * Get best score for a user
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @return Best score or null if no attempts
     */
    @Transactional(readOnly = true)
    public Double getBestScore(UUID userId, UUID quizId) {
        return quizAttemptRepository.findBestAttempt(userId, quizId)
            .map(QuizAttempt::getScore)
            .orElse(null);
    }

    /**
     * Check if user has passed the quiz
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @return True if user has passed
     */
    @Transactional(readOnly = true)
    public boolean hasUserPassedQuiz(UUID userId, UUID quizId) {
        return quizAttemptRepository.hasUserPassedQuiz(userId, quizId);
    }

    /**
     * Get attempts by enrollment
     *
     * @param enrollmentId The enrollment ID
     * @return List of quiz attempts
     */
    @Transactional(readOnly = true)
    public List<QuizAttempt> getAttemptsByEnrollment(UUID enrollmentId) {
        return quizAttemptRepository.findByEnrollmentIdAndDeletedFalse(enrollmentId);
    }

    /**
     * Get submitted attempts for a quiz (for instructor review)
     *
     * @param quizId The quiz ID
     * @param pageable Pagination parameters
     * @return Page of submitted attempts
     */
    @Transactional(readOnly = true)
    public Page<QuizAttempt> getSubmittedAttempts(UUID quizId, Pageable pageable) {
        return quizAttemptRepository.findSubmittedAttempts(quizId, pageable);
    }

    /**
     * Get in-progress attempts for a user
     *
     * @param userId The user ID
     * @return List of in-progress attempts
     */
    @Transactional(readOnly = true)
    public List<QuizAttempt> getInProgressAttempts(UUID userId) {
        return quizAttemptRepository.findInProgressAttempts(userId);
    }

    /**
     * Get quiz statistics
     *
     * @param quizId The quiz ID
     * @return Map with quiz statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuizStatistics(UUID quizId) {
        Map<String, Object> stats = new HashMap<>();

        long totalAttempts = quizAttemptRepository.countByUserIdAndQuizIdAndDeletedFalse(null, quizId);
        long passedAttempts = quizAttemptRepository.countPassedAttempts(quizId);
        Double averageScore = quizAttemptRepository.getAverageScore(quizId).orElse(0.0);

        stats.put("totalAttempts", totalAttempts);
        stats.put("passedAttempts", passedAttempts);
        stats.put("averageScore", averageScore);
        stats.put("passRate", totalAttempts > 0 ? (double) passedAttempts / totalAttempts * 100 : 0.0);

        return stats;
    }

    /**
     * Get next attempt number for user and quiz
     *
     * @param userId The user ID
     * @param quizId The quiz ID
     * @return Next attempt number
     */
    private int getNextAttemptNumber(UUID userId, UUID quizId) {
        Optional<QuizAttempt> latestAttempt = quizAttemptRepository
            .findFirstByUserIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(userId, quizId);

        return latestAttempt.map(attempt -> attempt.getAttemptNumber() + 1).orElse(1);
    }

    /**
     * Fire quiz completed event
     *
     * @param attempt The quiz attempt
     */
    private void fireQuizCompletedEvent(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Lesson lesson = quiz.getLesson();
        Course course = lesson.getModule().getCourse();

        QuizCompletedEvent event = new QuizCompletedEvent(
            attempt.getId(),
            attempt.getUser().getId(),
            quiz.getId(),
            quiz.getTitle(),
            course.getId(),
            course.getTitle(),
            attempt.getScore(),
            attempt.isPassed(),
            attempt.getAttemptNumber(),
            attempt.getSubmittedAt().atZone(ZoneId.systemDefault()).toInstant(),
            attempt.getEnrollment().getTenantId(),
            attempt.getUser().getId().toString()
        );

        eventPublisher.publishEvent(event);
        log.info("QuizCompletedEvent published for attempt: {}", attempt.getId());
    }

    /**
     * Get quiz by ID with questions
     *
     * @param quizId The quiz ID
     * @return Quiz with questions loaded
     */
    @Transactional(readOnly = true)
    public Quiz getQuizWithQuestions(UUID quizId) {
        return quizRepository.findByIdWithQuestions(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
    }

    /**
     * Get quiz by lesson ID
     *
     * @param lessonId The lesson ID
     * @return Quiz for the lesson
     */
    @Transactional(readOnly = true)
    public Optional<Quiz> getQuizByLesson(UUID lessonId) {
        return quizRepository.findByLessonIdAndDeletedFalse(lessonId);
    }

    /**
     * Get quizzes by course ID
     *
     * @param courseId The course ID
     * @return List of quizzes in the course
     */
    @Transactional(readOnly = true)
    public List<Quiz> getQuizzesByCourse(UUID courseId) {
        return quizRepository.findByCourseId(courseId);
    }

    /**
     * Count quizzes in a course
     *
     * @param courseId The course ID
     * @return Number of quizzes
     */
    @Transactional(readOnly = true)
    public long countQuizzesByCourse(UUID courseId) {
        return quizRepository.countByCourseId(courseId);
    }

    /**
     * Get attempt by ID
     *
     * @param attemptId The attempt ID
     * @return Quiz attempt
     */
    @Transactional(readOnly = true)
    public QuizAttempt getAttemptById(UUID attemptId) {
        return quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found: " + attemptId));
    }

    /**
     * Check if time limit is exceeded for an attempt
     *
     * @param attemptId The attempt ID
     * @return True if time limit exceeded
     */
    @Transactional(readOnly = true)
    public boolean isTimeLimitExceeded(UUID attemptId) {
        QuizAttempt attempt = getAttemptById(attemptId);
        return attempt.isTimeLimitExceeded(attempt.getQuiz().getTimeLimitMinutes());
    }

    /**
     * Get time remaining for an attempt
     *
     * @param attemptId The attempt ID
     * @return Time remaining in seconds, or null if no time limit
     */
    @Transactional(readOnly = true)
    public Long getTimeRemaining(UUID attemptId) {
        QuizAttempt attempt = getAttemptById(attemptId);
        return attempt.getTimeRemainingSeconds(attempt.getQuiz().getTimeLimitMinutes());
    }
}
