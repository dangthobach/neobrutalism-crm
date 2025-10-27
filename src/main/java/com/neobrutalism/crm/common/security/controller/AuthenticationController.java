package com.neobrutalism.crm.common.security.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.security.dto.ChangePasswordRequest;
import com.neobrutalism.crm.common.security.dto.LoginRequest;
import com.neobrutalism.crm.common.security.dto.LoginResponse;
import com.neobrutalism.crm.common.security.dto.RefreshTokenRequest;
import com.neobrutalism.crm.common.security.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication Controller
 * Handles authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and generate JWT tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change Password", description = "Change current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        authenticationService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout current user")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // In stateless JWT, logout is handled client-side by removing tokens
        // For token blacklisting, implement Redis-based token revocation
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User", description = "Get authenticated user information")
    public ResponseEntity<ApiResponse<UUID>> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved", userId));
    }
}
