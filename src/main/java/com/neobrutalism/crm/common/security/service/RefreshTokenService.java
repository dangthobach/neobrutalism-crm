package com.neobrutalism.crm.common.security.service;

import com.neobrutalism.crm.common.security.model.RefreshToken;
import com.neobrutalism.crm.common.security.repository.RefreshTokenRepository;
import com.neobrutalism.crm.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * ✅ PHASE 2.1: Refresh Token Service with Rotation
 * 
 * Implements secure refresh token pattern:
 * 1. Token Rotation: Each refresh generates a new token
 * 2. Reuse Detection: Detects if an old token is reused (attack)
 * 3. Device Tracking: Tracks devices for session management
 * 4. Suspicious Activity: Flags unusual patterns
 * 
 * Security Benefits:
 * - Limits damage from stolen refresh tokens
 * - Detects token theft early
 * - Allows per-device session management
 * - Automatic cleanup of old tokens
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token.expiration:2592000000}") // 30 days default
    private Long refreshTokenExpiration;

    @Value("${jwt.refresh-token.max-sessions-per-user:5}")
    private Integer maxSessionsPerUser;

    /**
     * Create a new refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(
        UUID userId,
        String username,
        String deviceId,
        String deviceName,
        String deviceType,
        String userAgent,
        String ipAddress
    ) {
        // Check max sessions limit
        long activeSessionsCount = refreshTokenRepository.countActiveSessionsByUserId(userId, Instant.now());
        if (activeSessionsCount >= maxSessionsPerUser) {
            log.warn("User {} has reached max sessions limit ({}). Revoking oldest session.",
                username, maxSessionsPerUser);
            revokeOldestSession(userId);
        }

        // Generate secure random token
        String tokenValue = generateSecureToken();
        String tokenHash = hashToken(tokenValue);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setJti(UUID.randomUUID().toString());
        refreshToken.setUserId(userId);
        refreshToken.setUsername(username);
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));
        refreshToken.setDeviceId(deviceId);
        refreshToken.setDeviceName(deviceName);
        refreshToken.setDeviceType(deviceType);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setRotationCount(0);

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user {} on device {}", username, deviceName);

        // Set plaintext token in transient field for return (never persisted to DB)
        saved.setPlaintextToken(tokenValue);
        return saved;
    }

    /**
     * Refresh tokens with rotation
     * Returns new access token and new refresh token
     */
    @Transactional
    public RefreshToken rotateRefreshToken(
        String oldToken,
        String userAgent,
        String ipAddress
    ) {
        String oldTokenHash = hashToken(oldToken);

        // Find old token
        RefreshToken oldRefreshToken = refreshTokenRepository
            .findByTokenHash(oldTokenHash)
            .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        // Security checks
        if (oldRefreshToken.isRevoked()) {
            // Token was already used! Possible theft detected
            log.error("Attempted reuse of revoked refresh token for user {}. Possible theft!",
                oldRefreshToken.getUsername());
            
            // Revoke ALL tokens for this user (security measure)
            revokeAllUserTokens(oldRefreshToken.getUserId(), "TOKEN_REUSE_DETECTED");
            
            throw new BusinessException("TOKEN_REUSE_DETECTED",
                "Refresh token reuse detected. All sessions have been terminated for security.");
        }

        if (oldRefreshToken.isExpired()) {
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        // Check for suspicious activity
        boolean isSuspicious = detectSuspiciousActivity(oldRefreshToken, ipAddress);
        if (isSuspicious) {
            log.warn("Suspicious refresh token usage detected for user {}", oldRefreshToken.getUsername());
        }

        // Create new refresh token (rotation)
        String newTokenValue = generateSecureToken();
        String newTokenHash = hashToken(newTokenValue);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setTokenHash(newTokenHash);
        newRefreshToken.setJti(UUID.randomUUID().toString());
        newRefreshToken.setUserId(oldRefreshToken.getUserId());
        newRefreshToken.setUsername(oldRefreshToken.getUsername());
        newRefreshToken.setIssuedAt(Instant.now());
        newRefreshToken.setExpiresAt(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));
        newRefreshToken.setDeviceId(oldRefreshToken.getDeviceId());
        newRefreshToken.setDeviceName(oldRefreshToken.getDeviceName());
        newRefreshToken.setDeviceType(oldRefreshToken.getDeviceType());
        newRefreshToken.setUserAgent(userAgent);
        newRefreshToken.setIpAddress(ipAddress);
        newRefreshToken.setRotationCount(oldRefreshToken.getRotationCount() + 1);
        newRefreshToken.setIsSuspicious(isSuspicious);

        RefreshToken saved = refreshTokenRepository.save(newRefreshToken);

        // Revoke old token and link to new one
        oldRefreshToken.replaceWith(saved.getId());
        oldRefreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(oldRefreshToken);

        log.info("Rotated refresh token for user {} (rotation count: {})",
            oldRefreshToken.getUsername(), newRefreshToken.getRotationCount());

        // Set plaintext token in transient field for return (never persisted to DB)
        saved.setPlaintextToken(newTokenValue);
        return saved;
    }

    /**
     * Validate refresh token
     */
    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new BusinessException("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        // Update last used timestamp
        refreshToken.setLastUsedAt(Instant.now());
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Revoke a refresh token
     */
    @Transactional
    public void revokeRefreshToken(String token, String reason) {
        String tokenHash = hashToken(token);
        
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshToken -> {
            refreshToken.revoke(reason);
            refreshTokenRepository.save(refreshToken);
            log.info("Revoked refresh token for user {} - Reason: {}",
                refreshToken.getUsername(), reason);
        });
    }

    /**
     * Revoke all tokens for a user
     */
    @Transactional
    public int revokeAllUserTokens(UUID userId, String reason) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(
            userId, Instant.now(), reason
        );
        log.warn("Revoked {} refresh tokens for user {} - Reason: {}",
            revokedCount, userId, reason);
        return revokedCount;
    }

    /**
     * Revoke all tokens for a device
     */
    @Transactional
    public int revokeAllDeviceTokens(String deviceId, String reason) {
        return refreshTokenRepository.revokeAllDeviceTokens(
            deviceId, Instant.now(), reason
        );
    }

    /**
     * Revoke oldest session for user (when max sessions reached)
     */
    @Transactional
    protected void revokeOldestSession(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository
            .findActiveTokensByUserId(userId, Instant.now());
        
        if (!activeTokens.isEmpty()) {
            RefreshToken oldest = activeTokens.get(activeTokens.size() - 1);
            oldest.revoke("MAX_SESSIONS_EXCEEDED");
            refreshTokenRepository.save(oldest);
            log.info("Revoked oldest session for user {} (max sessions limit)", userId);
        }
    }

    /**
     * Get all active sessions for a user
     */
    public List<RefreshToken> getUserActiveSessions(UUID userId) {
        return refreshTokenRepository.findActiveTokensByUserId(userId, Instant.now());
    }

    /**
     * Detect suspicious activity
     */
    private boolean detectSuspiciousActivity(RefreshToken token, String currentIpAddress) {
        boolean suspicious = false;
        StringBuilder reason = new StringBuilder();

        // Check IP mismatch
        if (token.getIpAddress() != null && !token.getIpAddress().equals(currentIpAddress)) {
            suspicious = true;
            reason.append("IP_MISMATCH;");
        }

        // Check high rotation count (possible abuse)
        if (token.getRotationCount() > 100) {
            suspicious = true;
            reason.append("HIGH_ROTATION_COUNT;");
        }

        // Check rapid successive refreshes (possible attack)
        if (token.getLastUsedAt() != null) {
            long minutesSinceLastUse = ChronoUnit.MINUTES.between(
                token.getLastUsedAt(), Instant.now()
            );
            if (minutesSinceLastUse < 1) {
                suspicious = true;
                reason.append("RAPID_REFRESH;");
            }
        }

        if (suspicious) {
            token.setSuspiciousReason(reason.toString());
        }

        return suspicious;
    }

    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash token using SHA-256
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
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
     * Cleanup expired and revoked tokens
     * Runs daily at 3 AM
     * Keep revoked tokens for 30 days for audit
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoffDate = Instant.now().minus(30, ChronoUnit.DAYS);
        int deletedCount = refreshTokenRepository.deleteExpiredRevokedTokens(cutoffDate);
        
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired refresh tokens", deletedCount);
        }
    }

    /**
     * Find and flag suspicious tokens
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Hourly
    @Transactional
    public void flagSuspiciousTokens() {
        // Find tokens with high rotation count
        List<RefreshToken> highRotationTokens = refreshTokenRepository
            .findHighRotationTokens(50);
        
        for (RefreshToken token : highRotationTokens) {
            if (!token.getIsSuspicious()) {
                token.setIsSuspicious(true);
                token.setSuspiciousReason("HIGH_ROTATION_COUNT: " + token.getRotationCount());
                refreshTokenRepository.save(token);
                log.warn("Flagged suspicious token for user {} - High rotation count: {}",
                    token.getUsername(), token.getRotationCount());
            }
        }
    }
}
