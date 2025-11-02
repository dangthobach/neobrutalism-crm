package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.StatefulEntity;
import com.neobrutalism.crm.common.enums.CourseLevel;
import com.neobrutalism.crm.common.enums.CourseStatus;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.content.model.ContentCategory;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Course entity for LMS
 * Uses state machine for lifecycle management
 */
@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_courses_tenant", columnList = "tenant_id"),
    @Index(name = "idx_courses_slug", columnList = "slug"),
    @Index(name = "idx_courses_status", columnList = "status"),
    @Index(name = "idx_courses_instructor", columnList = "instructor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course extends StatefulEntity<CourseStatus> {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "slug", nullable = false, length = 500)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "preview_video_url", length = 1000)
    private String previewVideoUrl;

    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    private String learningObjectives;

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_level", nullable = false, length = 50)
    private CourseLevel courseLevel = CourseLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_required", length = 50)
    private MemberTier tierRequired = MemberTier.FREE;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ContentCategory category;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "enrollment_count", nullable = false)
    private Integer enrollmentCount = 0;

    @Column(name = "completion_count", nullable = false)
    private Integer completionCount = 0;

    @Column(name = "rating_average", precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CourseModule> modules = new ArrayList<>();

    @Override
    protected CourseStatus getInitialStatus() {
        return CourseStatus.DRAFT;
    }

    @Override
    protected Set<CourseStatus> getAllowedTransitions(CourseStatus currentStatus) {
        return switch (currentStatus) {
            case DRAFT -> Set.of(CourseStatus.REVIEW, CourseStatus.DELETED);
            case REVIEW -> Set.of(CourseStatus.DRAFT, CourseStatus.PUBLISHED, CourseStatus.DELETED);
            case PUBLISHED -> Set.of(CourseStatus.DRAFT, CourseStatus.ARCHIVED, CourseStatus.DELETED);
            case ARCHIVED -> Set.of(CourseStatus.PUBLISHED, CourseStatus.DELETED);
            case DELETED -> Set.of();
        };
    }

    @Override
    protected void onStatusChanged(CourseStatus oldStatus, CourseStatus newStatus) {
        if (newStatus == CourseStatus.PUBLISHED && oldStatus != CourseStatus.PUBLISHED) {
            this.publishedAt = Instant.now();
        }
    }

    // Business methods

    /**
     * Submit course for review
     */
    public void submitForReview(String reviewedBy, String reason) {
        transitionTo(CourseStatus.REVIEW, reviewedBy, reason);
    }

    /**
     * Publish course
     */
    public void publish(String publishedBy, String reason) {
        transitionTo(CourseStatus.PUBLISHED, publishedBy, reason);
    }

    /**
     * Archive course
     */
    public void archive(String archivedBy, String reason) {
        transitionTo(CourseStatus.ARCHIVED, archivedBy, reason);
    }

    /**
     * Return to draft
     */
    public void returnToDraft(String changedBy, String reason) {
        transitionTo(CourseStatus.DRAFT, changedBy, reason);
    }

    /**
     * Increment enrollment count
     */
    public void incrementEnrollmentCount() {
        this.enrollmentCount++;
    }

    /**
     * Enroll student (alias for incrementEnrollmentCount)
     */
    public void enroll() {
        incrementEnrollmentCount();
    }

    /**
     * Increment completion count
     */
    public void incrementCompletionCount() {
        this.completionCount++;
    }

    /**
     * Update course rating
     */
    public void updateRating(int newRating) {
        if (this.ratingAverage == null) {
            this.ratingAverage = BigDecimal.valueOf(newRating);
            this.ratingCount = 1;
        } else {
            BigDecimal totalRating = this.ratingAverage.multiply(BigDecimal.valueOf(this.ratingCount));
            totalRating = totalRating.add(BigDecimal.valueOf(newRating));
            this.ratingCount++;
            this.ratingAverage = totalRating.divide(BigDecimal.valueOf(this.ratingCount), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Add module to course
     */
    public void addModule(CourseModule module) {
        this.modules.add(module);
        module.setCourse(this);
        module.setSortOrder(this.modules.size());
    }

    /**
     * Remove module from course
     */
    public void removeModule(CourseModule module) {
        this.modules.remove(module);
        module.setCourse(null);
        reorderModules();
    }

    /**
     * Reorder modules after removal
     */
    private void reorderModules() {
        for (int i = 0; i < this.modules.size(); i++) {
            this.modules.get(i).setSortOrder(i + 1);
        }
    }

    /**
     * Get total number of lessons across all modules
     */
    public int getTotalLessons() {
        return modules.stream()
            .mapToInt(CourseModule::getLessonCount)
            .sum();
    }

    /**
     * Get total duration in minutes
     */
    public int getTotalDurationMinutes() {
        return modules.stream()
            .mapToInt(CourseModule::getDurationMinutes)
            .sum();
    }

    /**
     * Check if course is published
     */
    public boolean isPublished() {
        return this.getStatus() == CourseStatus.PUBLISHED;
    }

    /**
     * Check if user can enroll (tier check)
     */
    public boolean canBeEnrolledBy(MemberTier userTier) {
        return userTier.canAccess(this.tierRequired);
    }

    /**
     * Calculate completion rate
     */
    public double getCompletionRate() {
        if (enrollmentCount == 0) return 0.0;
        return (double) completionCount / enrollmentCount * 100;
    }

    /**
     * Check if course is free
     */
    public boolean isFree() {
        return this.price == null || this.price.compareTo(BigDecimal.ZERO) == 0;
    }
}
