package com.neobrutalism.crm.domain.course.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.course.dto.QuizAttemptDTO;
import com.neobrutalism.crm.domain.course.dto.QuizDTO;
import com.neobrutalism.crm.domain.course.dto.SubmitQuizRequest;
import com.neobrutalism.crm.domain.course.model.Quiz;
import com.neobrutalism.crm.domain.course.model.QuizAttempt;
import com.neobrutalism.crm.domain.course.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for Quiz management
 */
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Quiz taking and grading APIs")
public class QuizController {

    private final QuizService quizService;

    /**
     * Get quiz by ID with questions
     */
    @GetMapping("/{quizId}")
    @Operation(summary = "Get quiz", description = "Get quiz with all questions")
    public ApiResponse<Quiz> getQuiz(@PathVariable UUID quizId) {
        Quiz quiz = quizService.getQuizWithQuestions(quizId);
        return ApiResponse.success(quiz);
    }

    /**
     * Get quiz by lesson ID
     */
    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get quiz by lesson", description = "Get quiz for a specific lesson")
    public ApiResponse<Quiz> getQuizByLesson(@PathVariable UUID lessonId) {
        Optional<Quiz> quiz = quizService.getQuizByLesson(lessonId);
        return quiz.map(ApiResponse::success)
                .orElse(ApiResponse.success("No quiz found for this lesson", null));
    }

    /**
     * Get quizzes by course
     */
    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get course quizzes", description = "Get all quizzes in a course")
    public ApiResponse<List<Quiz>> getCourseQuizzes(@PathVariable UUID courseId) {
        List<Quiz> quizzes = quizService.getQuizzesByCourse(courseId);
        return ApiResponse.success(quizzes);
    }

    /**
     * Start a new quiz attempt
     */
    @PostMapping("/{quizId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start quiz", description = "Create a new quiz attempt")
    public ApiResponse<QuizAttempt> startQuiz(
            @PathVariable UUID quizId,
            @RequestParam UUID enrollmentId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        QuizAttempt attempt = quizService.startQuizAttempt(userId, quizId, enrollmentId);
        return ApiResponse.success("Quiz started successfully", attempt);
    }

    /**
     * Submit answer for a question
     */
    @PostMapping("/attempts/{attemptId}/answer")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Submit answer", description = "Submit answer for a single question")
    public ApiResponse<Void> submitAnswer(
            @PathVariable UUID attemptId,
            @RequestParam UUID questionId,
            @RequestParam String answer) {

        quizService.submitAnswer(attemptId, questionId, answer);
        return ApiResponse.success("Answer submitted");
    }

    /**
     * Submit multiple answers at once
     */
    @PostMapping("/attempts/{attemptId}/answers")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Submit answers", description = "Submit multiple answers at once")
    public ApiResponse<Void> submitAnswers(
            @PathVariable UUID attemptId,
            @RequestBody Map<String, String> answers) {

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            UUID questionId = UUID.fromString(entry.getKey());
            quizService.submitAnswer(attemptId, questionId, entry.getValue());
        }
        return ApiResponse.success("Answers submitted");
    }

    /**
     * Submit and grade quiz
     */
    @PostMapping("/attempts/{attemptId}/submit")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Submit quiz", description = "Submit quiz for grading")
    public ApiResponse<QuizAttempt> submitQuiz(@PathVariable UUID attemptId) {

        QuizAttempt gradedAttempt = quizService.submitQuiz(attemptId);
        return ApiResponse.success("Quiz submitted and graded", gradedAttempt);
    }

    /**
     * Get quiz attempt by ID
     */
    @GetMapping("/attempts/{attemptId}")
    @Operation(summary = "Get attempt", description = "Get quiz attempt details")
    public ApiResponse<QuizAttempt> getAttempt(@PathVariable UUID attemptId) {
        QuizAttempt attempt = quizService.getAttemptById(attemptId);
        return ApiResponse.success(attempt);
    }

    /**
     * Get user's attempts for a quiz
     */
    @GetMapping("/{quizId}/attempts")
    @Operation(summary = "Get user attempts", description = "Get all attempts for a quiz by user")
    public ApiResponse<List<QuizAttempt>> getUserAttempts(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        List<QuizAttempt> attempts = quizService.getQuizAttempts(userId, quizId);
        return ApiResponse.success(attempts);
    }

    /**
     * Get latest attempt
     */
    @GetMapping("/{quizId}/attempts/latest")
    @Operation(summary = "Get latest attempt", description = "Get user's most recent attempt")
    public ApiResponse<QuizAttempt> getLatestAttempt(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        Optional<QuizAttempt> attempt = quizService.getLatestAttempt(userId, quizId);
        return attempt.map(ApiResponse::success)
                .orElse(ApiResponse.success("No attempts found", null));
    }

    /**
     * Get best attempt (highest score)
     */
    @GetMapping("/{quizId}/attempts/best")
    @Operation(summary = "Get best attempt", description = "Get user's highest scoring attempt")
    public ApiResponse<QuizAttempt> getBestAttempt(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        Optional<QuizAttempt> attempt = quizService.getBestAttempt(userId, quizId);
        return attempt.map(ApiResponse::success)
                .orElse(ApiResponse.success("No attempts found", null));
    }

    /**
     * Get best score
     */
    @GetMapping("/{quizId}/best-score")
    @Operation(summary = "Get best score", description = "Get user's best score for quiz")
    public ApiResponse<Double> getBestScore(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        Double score = quizService.getBestScore(userId, quizId);
        return ApiResponse.success(score);
    }

    /**
     * Check if user passed quiz
     */
    @GetMapping("/{quizId}/has-passed")
    @Operation(summary = "Check if passed", description = "Check if user has passed the quiz")
    public ApiResponse<Boolean> hasPassedQuiz(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        boolean hasPassed = quizService.hasUserPassedQuiz(userId, quizId);
        return ApiResponse.success(hasPassed);
    }

    /**
     * Get in-progress attempts
     */
    @GetMapping("/attempts/in-progress")
    @Operation(summary = "Get in-progress attempts", description = "Get user's incomplete quiz attempts")
    public ApiResponse<List<QuizAttempt>> getInProgressAttempts(
            @RequestHeader(value = "X-User-Id") UUID userId) {

        List<QuizAttempt> attempts = quizService.getInProgressAttempts(userId);
        return ApiResponse.success(attempts);
    }

    /**
     * Get all attempts for enrollment
     */
    @GetMapping("/enrollment/{enrollmentId}/attempts")
    @Operation(summary = "Get enrollment attempts", description = "Get all quiz attempts for enrollment")
    public ApiResponse<List<QuizAttempt>> getEnrollmentAttempts(
            @PathVariable UUID enrollmentId) {

        List<QuizAttempt> attempts = quizService.getAttemptsByEnrollment(enrollmentId);
        return ApiResponse.success(attempts);
    }

    /**
     * Get submitted attempts (for instructors)
     */
    @GetMapping("/{quizId}/attempts/submitted")
    @Operation(summary = "Get submitted attempts", description = "Get all submitted attempts for grading")
    public ApiResponse<Page<QuizAttempt>> getSubmittedAttempts(
            @PathVariable UUID quizId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<QuizAttempt> attempts = quizService.getSubmittedAttempts(quizId, pageable);
        return ApiResponse.success(attempts);
    }

    /**
     * Get quiz statistics
     */
    @GetMapping("/{quizId}/statistics")
    @Operation(summary = "Get statistics", description = "Get quiz attempt statistics")
    public ApiResponse<Map<String, Object>> getQuizStatistics(@PathVariable UUID quizId) {
        Map<String, Object> stats = quizService.getQuizStatistics(quizId);
        return ApiResponse.success(stats);
    }

    /**
     * Check time limit
     */
    @GetMapping("/attempts/{attemptId}/time-limit")
    @Operation(summary = "Check time limit", description = "Check if time limit exceeded")
    public ApiResponse<Boolean> isTimeLimitExceeded(@PathVariable UUID attemptId) {
        boolean exceeded = quizService.isTimeLimitExceeded(attemptId);
        return ApiResponse.success(exceeded);
    }

    /**
     * Get time remaining
     */
    @GetMapping("/attempts/{attemptId}/time-remaining")
    @Operation(summary = "Get time remaining", description = "Get remaining time for attempt")
    public ApiResponse<Long> getTimeRemaining(@PathVariable UUID attemptId) {
        Long seconds = quizService.getTimeRemaining(attemptId);
        return ApiResponse.success(seconds);
    }
}
