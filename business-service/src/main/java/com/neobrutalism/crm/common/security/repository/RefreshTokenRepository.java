package com.neobrutalism.crm.common.security.repository;

import com.neobrutalism.crm.common.security.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ PHASE 2.1: Refresh Token Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by its hash
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find refresh token by JWT ID
     */
    Optional<RefreshToken> findByJti(String jti);

    /**
     * Find all active (non-revoked, non-expired) tokens for a user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUserId(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );

    /**
     * Find all tokens for a user (including revoked)
     */
    List<RefreshToken> findByUserIdOrderByIssuedAtDesc(UUID userId);

    /**
     * Find tokens by device ID
     */
    List<RefreshToken> findByDeviceIdOrderByIssuedAtDesc(String deviceId);

    /**
     * Find suspicious tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.isSuspicious = true " +
           "AND rt.revoked = false ORDER BY rt.issuedAt DESC")
    List<RefreshToken> findSuspiciousTokens();

    /**
     * Count active sessions for a user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveSessionsByUserId(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );

    /**
     * Revoke all tokens for a user
     * Used when: password change, account locked, security breach
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, " +
           "rt.revokedReason = :reason WHERE rt.userId = :userId AND rt.revoked = false")
    int revokeAllUserTokens(
        @Param("userId") UUID userId,
        @Param("now") Instant now,
        @Param("reason") String reason
    );

    /**
     * Revoke all tokens for a device
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, " +
           "rt.revokedReason = :reason WHERE rt.deviceId = :deviceId AND rt.revoked = false")
    int revokeAllDeviceTokens(
        @Param("deviceId") String deviceId,
        @Param("now") Instant now,
        @Param("reason") String reason
    );

    /**
     * Cleanup expired and revoked tokens
     * Keep revoked tokens for 30 days for audit
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoffDate " +
           "AND rt.revoked = true")
    int deleteExpiredRevokedTokens(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Find tokens that were replaced (part of rotation chain)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.replacedByToken IS NOT NULL " +
           "ORDER BY rt.issuedAt DESC")
    List<RefreshToken> findRotatedTokens();

    /**
     * Check if token was reused (security attack detection)
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
           "FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash " +
           "AND rt.replacedByToken IS NOT NULL")
    boolean isTokenReused(@Param("tokenHash") String tokenHash);

    /**
     * Find tokens with high rotation count (potential abuse)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.rotationCount > :threshold " +
           "AND rt.revoked = false ORDER BY rt.rotationCount DESC")
    List<RefreshToken> findHighRotationTokens(@Param("threshold") Integer threshold);
}
