package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.common.enums.LessonProgressStatus;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lesson progress tracking entity
 */
@Entity
@Table(name = "lesson_progress",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_lesson_progress", columnNames = {"user_id", "lesson_id"})
    },
    indexes = {
        @Index(name = "idx_lesson_progress_user", columnList = "user_id"),
        @Index(name = "idx_lesson_progress_lesson", columnList = "lesson_id"),
        @Index(name = "idx_lesson_progress_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_lesson_progress_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgress extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LessonProgressStatus status = LessonProgressStatus.NOT_STARTED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_position_seconds")
    private Integer lastPositionSeconds;

    @Column(name = "completion_percentage", nullable = false)
    private Integer completionPercentage = 0;

    @Column(name = "time_spent_seconds", nullable = false)
    private Integer timeSpentSeconds = 0;

    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Business methods

    /**
     * Start lesson
     */
    public void start() {
        if (this.status == LessonProgressStatus.NOT_STARTED) {
            this.status = LessonProgressStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    /**
     * Complete lesson
     */
    public void complete() {
        this.status = LessonProgressStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completionPercentage = 100;
    }

    /**
     * Update video position
     */
    public void updateVideoPosition(int seconds) {
        if (this.status == LessonProgressStatus.NOT_STARTED) {
            start();
        }
        this.lastPositionSeconds = seconds;
    }

    /**
     * Update completion percentage
     */
    public void updateCompletionPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Completion percentage must be between 0 and 100");
        }

        if (this.status == LessonProgressStatus.NOT_STARTED && percentage > 0) {
            start();
        }

        this.completionPercentage = percentage;

        if (percentage == 100 && this.status != LessonProgressStatus.COMPLETED) {
            complete();
        }
    }

    /**
     * Add time spent
     */
    public void addTimeSpent(int seconds) {
        this.timeSpentSeconds += seconds;
    }

    /**
     * Increment attempts
     */
    public void incrementAttempts() {
        this.attemptsCount++;
    }

    /**
     * Check if lesson is completed
     */
    public boolean isCompleted() {
        return this.status == LessonProgressStatus.COMPLETED;
    }

    /**
     * Check if lesson is in progress
     */
    public boolean isInProgress() {
        return this.status == LessonProgressStatus.IN_PROGRESS;
    }

    /**
     * Check if lesson is started
     */
    public boolean isStarted() {
        return this.status != LessonProgressStatus.NOT_STARTED;
    }

    /**
     * Get time spent in minutes
     */
    public int getTimeSpentMinutes() {
        return timeSpentSeconds / 60;
    }

    /**
     * Get duration since start in days
     */
    public Long getDaysSinceStart() {
        if (startedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, LocalDateTime.now()).toDays();
    }
}
