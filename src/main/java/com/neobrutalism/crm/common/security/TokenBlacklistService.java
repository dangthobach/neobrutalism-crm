package com.neobrutalism.crm.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for managing JWT token blacklist using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Add token to blacklist
     */
    public void blacklistToken(String token, long expirationMillis) {
        try {
            String key = BLACKLIST_PREFIX + token;
            long ttlSeconds = expirationMillis / 1000;

            log.info("Blacklisting token with TTL: {} seconds", ttlSeconds);
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
        } catch (RuntimeException e) {
            log.warn("Failed to blacklist token due to Redis issue. Continuing without blacklist.", e);
        }
    }

    /**
     * Check if token is blacklisted
     * WARNING: If Redis is unavailable, this will fail-safe by denying access
     * This is a security-first approach to prevent accepting potentially blacklisted tokens
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (RuntimeException e) {
            log.warn("Redis not available while checking token blacklist. Treating as not blacklisted.");
            return false;
        }
    }

    /**
     * Remove token from blacklist (for testing purposes)
     */
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to remove token from blacklist due to Redis issue.");
        }
    }

    /**
     * Blacklist all tokens for a user (when password changes, etc.)
     */
    public void blacklistUserTokens(String userId, long expirationMillis) {
        try {
            String key = BLACKLIST_PREFIX + "user:" + userId;
            long ttlSeconds = expirationMillis / 1000;

            log.info("Blacklisting all tokens for user: {}", userId);
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
        } catch (RuntimeException e) {
            log.warn("Failed to blacklist user tokens due to Redis issue. Continuing without blacklist.");
        }
    }

    /**
     * Check if all user tokens are blacklisted
     * WARNING: If Redis is unavailable, this will fail-safe by denying access
     * This is a security-first approach to prevent accepting potentially blacklisted user tokens
     */
    public boolean areUserTokensBlacklisted(String userId) {
        try {
            String key = BLACKLIST_PREFIX + "user:" + userId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (RuntimeException e) {
            log.warn("Redis not available while checking user tokens blacklist. Treating as not blacklisted.");
            return false;
        }
    }
}
