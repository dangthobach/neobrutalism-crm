package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing refresh tokens with rotation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long refreshTokenExpiration;

    @Value("${jwt.max-refresh-tokens-per-user:5}")
    private int maxRefreshTokensPerUser;

    /**
     * Create a new refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(UUID userId, String ipAddress, String userAgent) {
        log.info("Creating refresh token for user: {}", userId);

        // Check if user has too many active tokens
        long activeTokenCount = refreshTokenRepository.countActiveTokensByUserId(userId, Instant.now());
        if (activeTokenCount >= maxRefreshTokensPerUser) {
            // Revoke oldest tokens
            List<RefreshToken> activeTokens = refreshTokenRepository.findActiveTokensByUserId(userId, Instant.now());
            activeTokens.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(activeTokenCount - maxRefreshTokensPerUser + 1)
                    .forEach(token -> {
                        token.setRevoked(true);
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    });
        }

        // Generate new token (pass tenantId as "default" or retrieve from context)
        String tokenValue = jwtTokenProvider.generateRefreshToken(userId, "default");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpiration));
        refreshToken.setCreatedByIp(ipAddress);
        refreshToken.setUserAgent(userAgent);

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Rotate refresh token (revoke old, create new)
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String token, String ipAddress, String userAgent) {
        log.info("Rotating refresh token");

        RefreshToken oldToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid refresh token"));

        if (!oldToken.isActive()) {
            throw new ValidationException("Refresh token is no longer active");
        }

        // Revoke old token
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now());
        oldToken.setRevokedByIp(ipAddress);

        // Create new token
        RefreshToken newToken = createRefreshToken(oldToken.getUserId(), ipAddress, userAgent);

        // Link old token to new token
        oldToken.setReplacedByToken(newToken.getToken());
        refreshTokenRepository.save(oldToken);

        return newToken;
    }

    /**
     * Validate refresh token
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            // Check if token was replaced (rotation)
            if (refreshToken.getReplacedByToken() != null) {
                throw new ValidationException("Token has been rotated. Please use the new token.");
            }
            throw new ValidationException("Refresh token is no longer active");
        }

        return refreshToken;
    }

    /**
     * Revoke refresh token
     */
    @Transactional
    public void revokeToken(String token, String ipAddress) {
        log.info("Revoking refresh token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid refresh token"));

        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshToken.setRevokedAt(Instant.now());
            refreshToken.setRevokedByIp(ipAddress);
            refreshTokenRepository.save(refreshToken);
        }
    }

    /**
     * Revoke all tokens for a user
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        log.info("Revoking all tokens for user: {}", userId);
        refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());
    }

    /**
     * Get all active tokens for a user
     */
    public List<RefreshToken> getUserActiveTokens(UUID userId) {
        return refreshTokenRepository.findActiveTokensByUserId(userId, Instant.now());
    }

    /**
     * Clean up expired tokens (scheduled daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired refresh tokens");
        Instant cutoffDate = Instant.now().minusSeconds(86400); // Keep for 1 day after expiry
        refreshTokenRepository.deleteExpiredTokens(cutoffDate);
    }
}
