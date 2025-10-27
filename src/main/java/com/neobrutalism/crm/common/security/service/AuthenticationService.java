package com.neobrutalism.crm.common.security.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.security.JwtTokenProvider;
import com.neobrutalism.crm.common.security.dto.ChangePasswordRequest;
import com.neobrutalism.crm.common.security.dto.LoginRequest;
import com.neobrutalism.crm.common.security.dto.LoginResponse;
import com.neobrutalism.crm.common.security.dto.RefreshTokenRequest;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.model.UserStatus;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication Service
 * Handles login, logout, token refresh, and password management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticate user and generate tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Set tenant context if provided
        if (request.getTenantId() != null) {
            TenantContext.setCurrentTenant(request.getTenantId());
        }

        try {
            // Find user by username
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BusinessException("Invalid username or password"));

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                // Record failed login attempt
                user.recordFailedLogin();
                userRepository.save(user);
                throw new BusinessException("Invalid username or password");
            }

            // Check if account is locked
            if (user.getStatus() == UserStatus.LOCKED) {
                if (user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
                    // Auto-unlock if lock period expired
                    user.transitionTo(UserStatus.ACTIVE, "system", "Lock period expired");
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                } else {
                    throw new BusinessException("Account is locked. Please try again later.");
                }
            }

            // Check if account is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException("Account is not active. Status: " + user.getStatus());
            }

            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());
            claims.put("firstName", user.getFirstName());
            claims.put("lastName", user.getLastName());

            String accessToken = jwtTokenProvider.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getTenantId(),
                    claims
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(),
                    user.getTenantId()
            );

            Instant expiresAt = jwtTokenProvider.getExpirationFromToken(accessToken);

            // Build response
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresAt(expiresAt)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .tenantId(user.getTenantId())
                    .build();

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        // Check token type
        String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException("Invalid token type. Expected refresh token.");
        }

        // Extract user info from refresh token
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String tenantId = jwtTokenProvider.getTenantIdFromToken(refreshToken);

        // Set tenant context
        TenantContext.setCurrentTenant(tenantId);

        try {
            // Fetch user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("User not found"));

            // Verify user is still active
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException("User account is not active");
            }

            // Generate new access token
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());
            claims.put("firstName", user.getFirstName());
            claims.put("lastName", user.getLastName());

            String newAccessToken = jwtTokenProvider.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getTenantId(),
                    claims
            );

            Instant expiresAt = jwtTokenProvider.getExpirationFromToken(newAccessToken);

            // Build response (keep same refresh token)
            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresAt(expiresAt)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .tenantId(user.getTenantId())
                    .build();

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Hash and update new password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    /**
     * Validate access token
     */
    public boolean validateAccessToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }

        String tokenType = jwtTokenProvider.getTokenTypeFromToken(token);
        return "access".equals(tokenType);
    }
}
