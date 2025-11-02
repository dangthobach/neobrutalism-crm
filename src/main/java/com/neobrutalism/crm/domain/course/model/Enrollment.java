package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.common.enums.EnrollmentStatus;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Enrollment entity - represents student enrollment in a course
 */
@Entity
@Table(name = "enrollments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_enrollment_user_course", columnNames = {"user_id", "course_id"})
    },
    indexes = {
        @Index(name = "idx_enrollments_user", columnList = "user_id"),
        @Index(name = "idx_enrollments_course", columnList = "course_id"),
        @Index(name = "idx_enrollments_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "price_paid", precision = 12, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "certificate_issued_at")
    private LocalDateTime certificateIssuedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Business methods

    /**
     * Mark enrollment as completed
     */
    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
    }

    /**
     * Mark enrollment as dropped
     */
    public void drop() {
        this.status = EnrollmentStatus.DROPPED;
    }

    /**
     * Suspend enrollment
     */
    public void suspend() {
        this.status = EnrollmentStatus.SUSPENDED;
    }

    /**
     * Reactivate enrollment
     */
    public void reactivate() {
        if (this.status != EnrollmentStatus.COMPLETED) {
            this.status = EnrollmentStatus.ACTIVE;
        }
    }

    /**
     * Update progress
     */
    public void updateProgress(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
        }
        this.progressPercentage = percentage;

        if (percentage == 100 && this.status == EnrollmentStatus.ACTIVE) {
            this.complete();
        }
    }

    /**
     * Update last accessed time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Check if enrollment is active
     */
    public boolean isActive() {
        return this.status == EnrollmentStatus.ACTIVE && !isExpired();
    }

    /**
     * Check if enrollment has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if enrollment is completed
     */
    public boolean isCompleted() {
        return this.status == EnrollmentStatus.COMPLETED;
    }

    /**
     * Check if certificate can be issued
     */
    public boolean canIssueCertificate() {
        return isCompleted() && certificateIssuedAt == null;
    }

    /**
     * Issue certificate
     */
    public void issueCertificate() {
        if (!canIssueCertificate()) {
            throw new IllegalStateException("Cannot issue certificate for this enrollment");
        }
        this.certificateIssuedAt = LocalDateTime.now();
    }

    /**
     * Get enrollment duration in days
     */
    public long getEnrollmentDurationDays() {
        LocalDateTime endDate = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(enrolledAt, endDate).toDays();
    }
}
