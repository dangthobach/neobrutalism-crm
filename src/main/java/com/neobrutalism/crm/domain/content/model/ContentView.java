package com.neobrutalism.crm.domain.content.model;

import com.neobrutalism.crm.common.entity.BaseEntity;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * Content view entity for tracking user engagement with content
 * Used for analytics and engagement scoring
 */
@Entity
@Table(name = "content_views", indexes = {
    @Index(name = "idx_content_views_content", columnList = "content_id"),
    @Index(name = "idx_content_views_user", columnList = "user_id"),
    @Index(name = "idx_content_views_session", columnList = "session_id"),
    @Index(name = "idx_content_views_viewed_at", columnList = "viewed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentView extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", length = 500)
    private String referrer;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;

    @Column(name = "scroll_percentage")
    private Integer scrollPercentage = 0;

    // Business methods

    /**
     * Check if view is from authenticated user
     */
    public boolean isAuthenticated() {
        return this.user != null;
    }

    /**
     * Check if user spent significant time (>30 seconds)
     */
    public boolean isSignificantView() {
        return this.timeSpentSeconds != null && this.timeSpentSeconds > 30;
    }

    /**
     * Check if user scrolled through most of the content (>70%)
     */
    public boolean isFullyRead() {
        return this.scrollPercentage != null && this.scrollPercentage > 70;
    }

    /**
     * Calculate engagement score for this view
     */
    public int calculateEngagementScore() {
        int score = 1; // Base score for viewing

        if (isAuthenticated()) {
            score += 2; // Bonus for authenticated users
        }

        if (isSignificantView()) {
            score += 3; // Bonus for spending time
        }

        if (isFullyRead()) {
            score += 4; // Bonus for reading thoroughly
        }

        return score;
    }
}
