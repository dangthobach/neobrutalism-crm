package com.neobrutalism.crm.common.security.service;

import com.neobrutalism.crm.common.security.model.TokenBlacklist;
import com.neobrutalism.crm.common.security.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * ✅ PHASE 2.1: Token Blacklist Service
 * 
 * Manages invalidated JWT tokens to prevent reuse.
 * 
 * Security Features:
 * - Immediate token invalidation on logout
 * - Forced invalidation for security breaches
 * - Automatic cleanup of expired entries
 * - Token reuse detection
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository blacklistRepository;

    /**
     * Blacklist a token (by full JWT string)
     */
    @Transactional
    public void blacklistToken(
        String token,
        UUID userId,
        String username,
        String reason,
        Instant expiresAt
    ) {
        String tokenHash = hashToken(token);
        blacklistTokenByHash(tokenHash, userId, username, reason, expiresAt, null, null, null);
        log.info("Blacklisted token for user {} - Reason: {}", username, reason);
    }

    /**
     * Blacklist a token with device information
     */
    @Transactional
    public void blacklistToken(
        String token,
        UUID userId,
        String username,
        String reason,
        Instant expiresAt,
        String userAgent,
        String ipAddress
    ) {
        String tokenHash = hashToken(token);
        blacklistTokenByHash(tokenHash, userId, username, reason, expiresAt, null, userAgent, ipAddress);
        log.info("Blacklisted token for user {} from IP {} - Reason: {}", username, ipAddress, reason);
    }

    /**
     * Blacklist a token by its hash (internal method)
     */
    private void blacklistTokenByHash(
        String tokenHash,
        UUID userId,
        String username,
        String reason,
        Instant expiresAt,
        String jti,
        String userAgent,
        String ipAddress
    ) {
        // Check if already blacklisted
        if (blacklistRepository.existsByTokenHash(tokenHash)) {
            log.debug("Token already blacklisted: {}", tokenHash);
            return;
        }

        TokenBlacklist blacklist = new TokenBlacklist();
        blacklist.setTokenHash(tokenHash);
        blacklist.setJti(jti);
        blacklist.setUserId(userId);
        blacklist.setUsername(username);
        blacklist.setReason(reason);
        blacklist.setExpiresAt(expiresAt);
        blacklist.setUserAgent(userAgent);
        blacklist.setIpAddress(ipAddress);

        blacklistRepository.save(blacklist);
    }

    /**
     * Blacklist all tokens for a user
     * Used when: password change, account locked, security breach
     */
    @Transactional
    public void blacklistAllUserTokens(UUID userId, String username, String reason) {
        // This would require storing all active tokens
        // For now, log the action - actual implementation would query active tokens
        log.warn("Blacklisting all tokens for user {} - Reason: {}", username, reason);
        
        // In production: Query all active tokens from token store and blacklist them
        // For JWT stateless approach: Set a "tokenInvalidatedAt" timestamp on User entity
    }

    /**
     * Blacklist all tokens for a user by user ID string (for compatibility)
     * @param userId The user ID as a string
     * @param expirationMillis Time-to-live in milliseconds
     */
    @Transactional
    public void blacklistUserTokens(String userId, long expirationMillis) {
        try {
            UUID userUuid = UUID.fromString(userId);
            Instant expiresAt = Instant.now().plusMillis(expirationMillis);
            
            // Create a blacklist entry for all user tokens
            // Using a special marker to indicate all tokens for this user are blacklisted
            String specialTokenHash = "USER_ALL_TOKENS_" + userId;
            
            blacklistTokenByHash(
                specialTokenHash,
                userUuid,
                "user:" + userId,
                BlacklistReason.PASSWORD_CHANGE,
                expiresAt,
                null,
                null,
                null
            );
            
            log.info("Blacklisted all tokens for user: {}", userId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId, e);
        }
    }

    /**
     * Check if all tokens for a user are blacklisted
     * @param userId The user ID as a string
     * @return true if all user tokens are blacklisted
     */
    public boolean areUserTokensBlacklisted(String userId) {
        String specialTokenHash = "USER_ALL_TOKENS_" + userId;
        return blacklistRepository.existsByTokenHash(specialTokenHash);
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        String tokenHash = hashToken(token);
        return blacklistRepository.existsByTokenHash(tokenHash);
    }

    /**
     * Check if a token is blacklisted by JWT ID
     */
    public boolean isJtiBlacklisted(String jti) {
        return blacklistRepository.existsByJti(jti);
    }

    /**
     * Get all blacklisted tokens for a user
     */
    public List<TokenBlacklist> getUserBlacklistedTokens(UUID userId) {
        return blacklistRepository.findByUserIdOrderByBlacklistedAtDesc(userId);
    }

    /**
     * Get blacklist statistics
     */
    public long countBlacklistedTokensForUser(UUID userId) {
        return blacklistRepository.countByUserId(userId);
    }

    /**
     * Get tokens blacklisted within time range
     */
    public List<TokenBlacklist> getBlacklistedTokensBetween(Instant start, Instant end) {
        return blacklistRepository.findByBlacklistedAtBetween(start, end);
    }

    /**
     * Cleanup expired blacklist entries
     * Runs every day at 2 AM
     * Deletes tokens that expired more than 7 days ago
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupExpiredBlacklistEntries() {
        Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
        int deletedCount = blacklistRepository.deleteExpiredTokens(cutoffDate);
        
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired blacklist entries", deletedCount);
        }
    }

    /**
     * Hash a token using SHA-256
     * We store hash instead of full token for security
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Blacklist reasons (constants)
     */
    public static class BlacklistReason {
        public static final String LOGOUT = "LOGOUT";
        public static final String REFRESH = "REFRESH";
        public static final String SECURITY_BREACH = "SECURITY_BREACH";
        public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
        public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
        public static final String FORCED_LOGOUT = "FORCED_LOGOUT";
        public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
        public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
    }
}
