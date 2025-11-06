package com.neobrutalism.crm.common.security.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * ✅ PHASE 2.1: JWT Blacklist for invalidated tokens
 * 
 * Stores hashes of invalidated JWT tokens to prevent reuse.
 * Used for:
 * - Logout (invalidate access token immediately)
 * - Token refresh (old token should not be usable)
 * - Security breaches (force invalidation)
 * - Password changes (invalidate all tokens)
 */
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_blacklist_token_hash", columnList = "token_hash"),
    @Index(name = "idx_blacklist_user", columnList = "user_id"),
    @Index(name = "idx_blacklist_expires_at", columnList = "expires_at"),
    @Index(name = "idx_blacklist_reason", columnList = "reason")
})
@Getter
@Setter
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-256 hash of the JWT token
     * We store hash instead of full token for security
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * JWT ID (jti claim) for quick lookup
     */
    @Column(name = "jti", length = 36)
    private String jti;

    /**
     * User who owned this token
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", length = 50)
    private String username;

    /**
     * Why was this token blacklisted?
     * - LOGOUT: User logged out
     * - REFRESH: Token was refreshed (old token invalid)
     * - SECURITY_BREACH: Security incident
     * - PASSWORD_CHANGE: User changed password
     * - FORCED: Admin forced logout
     */
    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    /**
     * When was this token blacklisted
     */
    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    /**
     * Original token expiry date
     * Used for automatic cleanup (delete expired blacklist entries)
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Device information (optional)
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Who initiated the blacklisting (for audit)
     */
    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        if (blacklistedAt == null) {
            blacklistedAt = Instant.now();
        }
    }
}
