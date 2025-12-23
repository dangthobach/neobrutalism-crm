package com.neobrutalism.crm.common.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Login Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login response containing JWT tokens and user information")
public class LoginResponse {

    @Schema(description = "JWT access token. Copy this value and use in Swagger 'Authorize' button as: Bearer <token>", 
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
    private String accessToken;
    
    @Schema(description = "Refresh token for getting new access tokens", example = "refresh_token_hash")
    private String refreshToken;
    
    @Schema(description = "Token type", example = "Bearer")
    @lombok.Builder.Default
    private String tokenType = "Bearer";
    
    @Schema(description = "Token expiry time in seconds", example = "3600")
    private Long expiresIn; // Token expiry in seconds
    
    @Schema(description = "Token expiry timestamp")
    private Instant expiresAt;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
    
    @Schema(description = "Username", example = "admin")
    private String username;
    
    @Schema(description = "User email", example = "admin@example.com")
    private String email;
    
    @Schema(description = "First name")
    private String firstName;
    
    @Schema(description = "Last name")
    private String lastName;
    
    @Schema(description = "Full name", example = "John Doe")
    private String fullName;
    
    @Schema(description = "Tenant ID", example = "default")
    private String tenantId;
    
    @Schema(description = "User roles", example = "[\"ADMIN\", \"USER\"]")
    private Set<String> roles;
}
