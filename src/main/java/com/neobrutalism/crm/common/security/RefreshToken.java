package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh Token entity for token rotation
 */
@Getter
@Setter
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token", columnList = "token"),
        @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
    }
)
public class RefreshToken extends AuditableEntity {

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token", length = 500)
    private String replacedByToken;

    @Column(name = "created_by_ip", length = 50)
    private String createdByIp;

    @Column(name = "revoked_by_ip", length = 50)
    private String revokedByIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }
}
