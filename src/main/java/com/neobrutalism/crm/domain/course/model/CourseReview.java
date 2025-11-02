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
 * Course review entity - student ratings and reviews
 */
@Entity
@Table(name = "course_reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_review", columnNames = {"user_id", "course_id"})
    },
    indexes = {
        @Index(name = "idx_course_reviews_user", columnList = "user_id"),
        @Index(name = "idx_course_reviews_course", columnList = "course_id"),
        @Index(name = "idx_course_reviews_rating", columnList = "rating")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseReview extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "is_verified_purchase", nullable = false)
    private Boolean isVerifiedPurchase = false;

    @Column(name = "helpful_count", nullable = false)
    private Integer helpfulCount = 0;

    @Column(name = "reported_count", nullable = false)
    private Integer reportedCount = 0;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt = LocalDateTime.now();

    @Column(name = "instructor_response", columnDefinition = "TEXT")
    private String instructorResponse;

    @Column(name = "instructor_responded_at")
    private LocalDateTime instructorRespondedAt;

    // Business methods

    /**
     * Validate rating
     */
    public void validateRating() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    /**
     * Mark as helpful
     */
    public void markAsHelpful() {
        this.helpfulCount++;
    }

    /**
     * Report review
     */
    public void report() {
        this.reportedCount++;
    }

    /**
     * Feature review
     */
    public void feature() {
        this.isFeatured = true;
    }

    /**
     * Unfeature review
     */
    public void unfeature() {
        this.isFeatured = false;
    }

    /**
     * Add instructor response
     */
    public void addInstructorResponse(String response) {
        this.instructorResponse = response;
        this.instructorRespondedAt = LocalDateTime.now();
    }

    /**
     * Remove instructor response
     */
    public void removeInstructorResponse() {
        this.instructorResponse = null;
        this.instructorRespondedAt = null;
    }

    /**
     * Check if review has instructor response
     */
    public boolean hasInstructorResponse() {
        return instructorResponse != null && !instructorResponse.trim().isEmpty();
    }

    /**
     * Check if review is positive
     */
    public boolean isPositive() {
        return rating >= 4;
    }

    /**
     * Check if review is negative
     */
    public boolean isNegative() {
        return rating <= 2;
    }

    /**
     * Get days since review
     */
    public long getDaysSinceReview() {
        return java.time.Duration.between(reviewedAt, LocalDateTime.now()).toDays();
    }
}
