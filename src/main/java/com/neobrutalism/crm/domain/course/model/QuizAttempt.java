package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quiz attempt entity - tracks student quiz submissions
 */
@Entity
@Table(name = "quiz_attempts", indexes = {
    @Index(name = "idx_quiz_attempts_user", columnList = "user_id"),
    @Index(name = "idx_quiz_attempts_quiz", columnList = "quiz_id"),
    @Index(name = "idx_quiz_attempts_enrollment", columnList = "enrollment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "score", precision = 5, scale = 2)
    private Double score;

    @Column(name = "points_earned")
    private Integer pointsEarned;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "is_passed")
    private Boolean isPassed;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @ElementCollection
    @CollectionTable(
        name = "quiz_attempt_answers",
        joinColumns = @JoinColumn(name = "attempt_id"),
        indexes = @Index(name = "idx_attempt_answers", columnList = "attempt_id")
    )
    @MapKeyColumn(name = "question_id")
    @Column(name = "answer", columnDefinition = "TEXT")
    private Map<String, String> answers = new HashMap<>();

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    // Business methods

    /**
     * Submit quiz attempt
     */
    public void submit() {
        if (this.submittedAt != null) {
            throw new IllegalStateException("Quiz attempt already submitted");
        }
        this.submittedAt = LocalDateTime.now();
        calculateTimeSpent();
    }

    /**
     * Calculate time spent
     */
    private void calculateTimeSpent() {
        if (startedAt != null && submittedAt != null) {
            this.timeSpentSeconds = (int) java.time.Duration.between(startedAt, submittedAt).getSeconds();
        }
    }

    /**
     * Grade the quiz attempt
     */
    public void grade(int pointsEarned, int totalPoints, int passingScore) {
        this.pointsEarned = pointsEarned;
        this.totalPoints = totalPoints;

        if (totalPoints > 0) {
            this.score = (double) pointsEarned / totalPoints * 100;
            this.isPassed = this.score >= passingScore;
        } else {
            this.score = 0.0;
            this.isPassed = false;
        }
    }

    /**
     * Add answer to quiz
     */
    public void addAnswer(String questionId, String answer) {
        if (this.submittedAt != null) {
            throw new IllegalStateException("Cannot modify submitted quiz");
        }
        this.answers.put(questionId, answer);
    }

    /**
     * Get answer for question
     */
    public String getAnswer(String questionId) {
        return this.answers.get(questionId);
    }

    /**
     * Check if quiz is submitted
     */
    public boolean isSubmitted() {
        return this.submittedAt != null;
    }

    /**
     * Check if quiz is passed
     */
    public boolean isPassed() {
        return Boolean.TRUE.equals(this.isPassed);
    }

    /**
     * Check if time limit exceeded
     */
    public boolean isTimeLimitExceeded(Integer timeLimitMinutes) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) {
            return false;
        }

        if (submittedAt != null) {
            return timeSpentSeconds > (timeLimitMinutes * 60);
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsElapsed = java.time.Duration.between(startedAt, now).getSeconds();
        return secondsElapsed > (timeLimitMinutes * 60);
    }

    /**
     * Get time remaining in seconds
     */
    public Long getTimeRemainingSeconds(Integer timeLimitMinutes) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) {
            return null;
        }

        if (submittedAt != null) {
            return 0L;
        }

        long secondsElapsed = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
        long timeLimit = timeLimitMinutes * 60L;
        return Math.max(0, timeLimit - secondsElapsed);
    }

    /**
     * Get completion percentage
     */
    public int getCompletionPercentage() {
        if (quiz == null || quiz.getQuestionCount() == 0) {
            return 0;
        }
        return (answers.size() * 100) / quiz.getQuestionCount();
    }

    /**
     * Get time spent in minutes
     */
    public int getTimeSpentMinutes() {
        return timeSpentSeconds != null ? timeSpentSeconds / 60 : 0;
    }
}
