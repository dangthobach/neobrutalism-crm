package com.neobrutalism.crm.common.security.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * ✅ PHASE 2.1: Refresh Token with rotation support
 * 
 * Implements secure refresh token pattern:
 * - Long-lived tokens for "remember me" functionality
 * - Token rotation: Each use generates a new refresh token
 * - Revocation support for security
 * - Device tracking for session management
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_hash", columnList = "token_hash"),
    @Index(name = "idx_refresh_jti", columnList = "jti"),
    @Index(name = "idx_refresh_user", columnList = "user_id"),
    @Index(name = "idx_refresh_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_revoked", columnList = "revoked"),
    @Index(name = "idx_refresh_device_id", columnList = "device_id"),
    @Index(name = "idx_refresh_suspicious", columnList = "is_suspicious")
})
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-256 hash of the refresh token
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * JWT ID (jti claim)
     */
    @Column(name = "jti", unique = true, length = 36)
    private String jti;

    /**
     * User who owns this token
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", length = 50)
    private String username;

    // ========================================
    // Token Lifecycle
    // ========================================

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    // ========================================
    // Revocation
    // ========================================

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 200)
    private String revokedReason;

    // ========================================
    // Token Rotation Tracking
    // ========================================

    /**
     * Points to the new token that replaced this one
     * Used to detect token reuse attacks
     */
    @Column(name = "replaced_by_token")
    private UUID replacedByToken;

    /**
     * How many times has this token chain been rotated
     */
    @Column(name = "rotation_count", nullable = false)
    private Integer rotationCount = 0;

    // ========================================
    // Device/Session Information
    // ========================================

    /**
     * Unique device identifier
     */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 200)
    private String deviceName;

    /**
     * Device type: WEB, MOBILE_IOS, MOBILE_ANDROID, DESKTOP
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "location", length = 200)
    private String location; // City, Country

    // ========================================
    // Security Flags
    // ========================================

    /**
     * Flag for suspicious activity
     * - Multiple failed refresh attempts
     * - IP mismatch
     * - Unusual location
     * - Concurrent usage
     */
    @Column(name = "is_suspicious", nullable = false)
    private Boolean isSuspicious = false;

    @Column(name = "suspicious_reason", columnDefinition = "TEXT")
    private String suspiciousReason;

    // ========================================
    // Audit
    // ========================================

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
        if (rotationCount == null) {
            rotationCount = 0;
        }
    }

    /**
     * Revoke this token
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    /**
     * Mark as replaced by another token
     */
    public void replaceWith(UUID newTokenId) {
        this.replacedByToken = newTokenId;
        this.revoke("ROTATED");
    }

    /**
     * Check if token is valid (not expired, not revoked)
     */
    public boolean isValid() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
