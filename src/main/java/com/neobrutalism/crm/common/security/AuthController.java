package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.security.dto.LoginRequest;
import com.neobrutalism.crm.common.security.dto.LoginResponse;
import com.neobrutalism.crm.common.security.dto.RefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles login, logout, and token refresh endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login and get JWT token",
        description = "Authenticate user with username/email and password. Returns JWT access token and refresh token. " +
                      "Copy the 'accessToken' value and use it in the 'Authorize' button at the top of Swagger UI. " +
                      "Format: Bearer <accessToken>",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful. Copy 'accessToken' from response and use in Authorize button.",
            content = @Content(
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = "{\n" +
                            "  \"success\": true,\n" +
                            "  \"message\": \"Login successful\",\n" +
                            "  \"data\": {\n" +
                            "    \"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n" +
                            "    \"refreshToken\": \"refresh_token_hash\",\n" +
                            "    \"tokenType\": \"Bearer\",\n" +
                            "    \"expiresIn\": 3600,\n" +
                            "    \"userId\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                            "    \"username\": \"admin\",\n" +
                            "    \"email\": \"admin@example.com\",\n" +
                            "    \"roles\": [\"ADMIN\"]\n" +
                            "  }\n" +
                            "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid credentials or validation error"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication failed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "423",
            description = "Account is locked"
        )
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Login credentials",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(
                        name = "Example Login",
                        value = "{\n" +
                                "  \"username\": \"admin\",\n" +
                                "  \"password\": \"password123\",\n" +
                                "  \"tenantId\": \"default\",\n" +
                                "  \"rememberMe\": false\n" +
                                "}"
                    )
                )
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Login request for: {}", request.getUsername());
        LoginResponse response = authenticationService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Refresh token request");
        LoginResponse response = authenticationService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = "Logout user and invalidate all refresh tokens. Requires authentication.",
        tags = {"Authentication"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logout successful"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Not authenticated"
        )
    })
    public ResponseEntity<ApiResponse<String>> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal != null) {
            log.info("Logout request for user: {}", userPrincipal.getUsername());
            authenticationService.logout(userPrincipal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get Current User",
        description = "Get currently authenticated user information. Requires authentication.",
        tags = {"Authentication"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User information retrieved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Not authenticated"
        )
    })
    public ResponseEntity<ApiResponse<UserPrincipal>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            log.debug("Get current user info requested but no authenticated principal");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated", "UNAUTHORIZED"));
        }
        log.debug("Get current user info: {}", userPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User info retrieved", userPrincipal));
    }

    /**
     * Check authentication status
     */
    @GetMapping("/status")
    @Operation(summary = "Check Auth Status", description = "Check if user is authenticated")
    public ResponseEntity<ApiResponse<Boolean>> checkAuthStatus(Authentication authentication) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        return ResponseEntity.ok(ApiResponse.success("Authentication status checked", isAuthenticated));
    }
}
