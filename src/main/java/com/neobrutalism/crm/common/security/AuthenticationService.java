package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.common.security.dto.LoginRequest;
import com.neobrutalism.crm.common.security.dto.LoginResponse;
import com.neobrutalism.crm.common.security.dto.RefreshTokenRequest;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * AuthenticationService - Handles user authentication (login, logout, refresh token)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserSessionService userSessionService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Authenticate user and generate JWT tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());

        // Find user by username or email
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .or(() -> userRepository.findByEmailAndDeletedFalse(request.getUsername()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login attempt for locked account: {}", user.getUsername());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Check if account is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login attempt for non-active account: {} (status: {})",
                    user.getUsername(), user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", user.getUsername());
            user.recordFailedLogin();
            userRepository.save(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Get client IP and user agent
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Record successful login
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);

        // Load user roles
        Set<String> roles = userSessionService.getUserRoles(user.getId());

        // Generate access token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTenantId(),
                roles
        );

        // Create refresh token with rotation support
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                ipAddress,
                userAgent
        );

        log.info("User logged in successfully: {} (tenant: {})", user.getUsername(), user.getTenantId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    /**
     * Refresh access token using refresh token with rotation
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        log.info("Refresh token request");

        String tokenString = request.getRefreshToken();
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Validate and rotate refresh token
        RefreshToken oldToken = refreshTokenService.validateRefreshToken(tokenString);
        RefreshToken newToken = refreshTokenService.rotateRefreshToken(
                tokenString,
                ipAddress,
                userAgent
        );

        // Load user
        User user = userRepository.findByIdAndDeletedFalse(oldToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Check if account is still active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Load user roles
        Set<String> roles = userSessionService.getUserRoles(user.getId());

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTenantId(),
                roles
        );

        log.info("Token refreshed successfully for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newToken.getToken()) // Return new rotated refresh token
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    /**
     * Logout user (clear cache, revoke tokens, and blacklist)
     */
    @Transactional
    public void logout(UUID userId) {
        log.info("Logout user: {}", userId);

        // Clear user session cache
        userSessionService.clearUserSession(userId);

        // Revoke all refresh tokens for the user
        refreshTokenService.revokeAllUserTokens(userId);

        // Blacklist all current tokens for the user (24 hour TTL)
        tokenBlacklistService.blacklistUserTokens(userId.toString(), 86400000L);
    }

    /**
     * Change password and invalidate all tokens
     */
    @Transactional
    public void changePassword(UUID userId, String newPassword) {
        log.info("Changing password for user: {}", userId);

        // Update password hash
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all existing tokens
        logout(userId);

        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Return first IP if multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
