package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * User achievement entity - tracks achievements earned by users
 */
@Entity
@Table(name = "user_achievements",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_achievement", columnNames = {"user_id", "achievement_id"})
    },
    indexes = {
        @Index(name = "idx_user_achievements_user", columnList = "user_id"),
        @Index(name = "idx_user_achievements_achievement", columnList = "achievement_id"),
        @Index(name = "idx_user_achievements_earned", columnList = "earned_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt = LocalDateTime.now();

    @Column(name = "progress", nullable = false)
    private Integer progress = 0;

    @Column(name = "is_notified", nullable = false)
    private Boolean isNotified = false;

    @Column(name = "is_displayed", nullable = false)
    private Boolean isDisplayed = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Business methods

    /**
     * Mark as notified
     */
    public void markAsNotified() {
        this.isNotified = true;
    }

    /**
     * Hide from profile
     */
    public void hide() {
        this.isDisplayed = false;
    }

    /**
     * Show on profile
     */
    public void show() {
        this.isDisplayed = true;
    }

    /**
     * Update progress
     */
    public void updateProgress(int newProgress) {
        if (newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        this.progress = newProgress;
    }

    /**
     * Check if achievement is complete
     */
    public boolean isComplete() {
        return progress >= 100;
    }

    /**
     * Check if recently earned (within 7 days)
     */
    public boolean isRecentlyEarned() {
        return java.time.Duration.between(earnedAt, LocalDateTime.now()).toDays() <= 7;
    }

    /**
     * Get days since earned
     */
    public long getDaysSinceEarned() {
        return java.time.Duration.between(earnedAt, LocalDateTime.now()).toDays();
    }

    /**
     * Get achievement points
     */
    public Integer getPoints() {
        return achievement != null ? achievement.getPoints() : 0;
    }
}
