package com.neobrutalism.crm.common.security.repository;

import com.neobrutalism.crm.common.security.model.TokenBlacklist;
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
 * ✅ PHASE 2.1: Token Blacklist Repository
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    /**
     * Check if a token is blacklisted by its hash
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Check if a token is blacklisted by JWT ID
     */
    boolean existsByJti(String jti);

    /**
     * Find blacklist entry by token hash
     */
    Optional<TokenBlacklist> findByTokenHash(String tokenHash);

    /**
     * Find blacklist entry by JWT ID
     */
    Optional<TokenBlacklist> findByJti(String jti);

    /**
     * Find all blacklisted tokens for a user
     */
    List<TokenBlacklist> findByUserIdOrderByBlacklistedAtDesc(UUID userId);

    /**
     * Find tokens blacklisted within a time range
     */
    @Query("SELECT tb FROM TokenBlacklist tb WHERE tb.blacklistedAt BETWEEN :start AND :end")
    List<TokenBlacklist> findByBlacklistedAtBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Find all tokens blacklisted for a specific reason
     */
    List<TokenBlacklist> findByReason(String reason);

    /**
     * Cleanup expired blacklist entries (run periodically)
     * Deletes tokens that expired more than 7 days ago
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :cutoffDate")
    int deleteExpiredTokens(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count blacklisted tokens for a user
     */
    long countByUserId(UUID userId);

    /**
     * Find tokens blacklisted by a specific admin
     */
    List<TokenBlacklist> findByCreatedByOrderByBlacklistedAtDesc(UUID adminId);
}
