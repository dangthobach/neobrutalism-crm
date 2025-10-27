package com.neobrutalism.crm.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token Provider
 * Handles token generation, validation, and extraction
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:neobrutalism-crm-secret-key-change-this-in-production-min-256-bits}") String secret,
            @Value("${jwt.access-token-validity:3600000}") long accessTokenValidityMs, // 1 hour
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityMs // 7 days
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /**
     * Generate access token
     */
    public String generateAccessToken(UUID userId, String username, String tenantId, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(accessTokenValidityMs);

        Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("sub", userId.toString());
        tokenClaims.put("username", username);
        tokenClaims.put("tenantId", tenantId);
        tokenClaims.put("type", "access");
        if (claims != null) {
            tokenClaims.putAll(claims);
        }

        return Jwts.builder()
                .claims(tokenClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(UUID userId, String tenantId) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(refreshTokenValidityMs);

        Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("sub", userId.toString());
        tokenClaims.put("tenantId", tenantId);
        tokenClaims.put("type", "refresh");

        return Jwts.builder()
                .claims(tokenClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extract user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Extract tenant ID from token
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tenantId", String.class);
    }

    /**
     * Extract token type from token
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * Get all claims from token
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get expiration time from token
     */
    public Instant getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().toInstant();
    }
}
