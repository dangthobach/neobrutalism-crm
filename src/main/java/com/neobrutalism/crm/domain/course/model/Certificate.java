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
 * Certificate entity - course completion certificates
 */
@Entity
@Table(name = "certificates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_certificate_number", columnNames = {"certificate_number"})
    },
    indexes = {
        @Index(name = "idx_certificates_user", columnList = "user_id"),
        @Index(name = "idx_certificates_course", columnList = "course_id"),
        @Index(name = "idx_certificates_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_certificates_issued", columnList = "issued_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Certificate extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 100)
    private String certificateNumber;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Column(name = "verification_url", length = 1000)
    private String verificationUrl;

    @Column(name = "final_score", precision = 5, scale = 2)
    private Double finalScore;

    @Column(name = "completion_date", nullable = false)
    private LocalDateTime completionDate;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = true;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // Business methods

    /**
     * Generate certificate number
     */
    public static String generateCertificateNumber(String courseCode, Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("CERT-%s-%d-%s", courseCode, userId, timestamp.substring(timestamp.length() - 6));
    }

    /**
     * Check if certificate is valid
     */
    public boolean isValid() {
        if (revokedAt != null) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Check if certificate is expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if certificate is revoked
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Revoke certificate
     */
    public void revoke(String reason) {
        if (isRevoked()) {
            throw new IllegalStateException("Certificate is already revoked");
        }
        this.revokedAt = LocalDateTime.now();
        this.revokeReason = reason;
        this.isVerified = false;
    }

    /**
     * Restore certificate
     */
    public void restore() {
        if (!isRevoked()) {
            throw new IllegalStateException("Certificate is not revoked");
        }
        this.revokedAt = null;
        this.revokeReason = null;
        this.isVerified = true;
    }

    /**
     * Set PDF URL
     */
    public void setPdfGenerated(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    /**
     * Set verification URL
     */
    public void setVerification(String verificationUrl) {
        this.verificationUrl = verificationUrl;
    }

    /**
     * Get certificate age in days
     */
    public long getCertificateAgeDays() {
        return java.time.Duration.between(issuedAt, LocalDateTime.now()).toDays();
    }

    /**
     * Get days until expiration
     */
    public Long getDaysUntilExpiration() {
        if (expiresAt == null) {
            return null;
        }
        long days = java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
        return Math.max(0, days);
    }

    /**
     * Check if certificate needs renewal (expires within 30 days)
     */
    public boolean needsRenewal() {
        if (expiresAt == null) {
            return false;
        }
        Long daysUntilExpiration = getDaysUntilExpiration();
        return daysUntilExpiration != null && daysUntilExpiration <= 30;
    }
}
