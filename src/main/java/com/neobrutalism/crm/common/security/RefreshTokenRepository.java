package com.neobrutalism.crm.common.security;

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
 * Repository for RefreshToken
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserId(UUID userId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :date")
    void deleteExpiredTokens(@Param("date") Instant date);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
