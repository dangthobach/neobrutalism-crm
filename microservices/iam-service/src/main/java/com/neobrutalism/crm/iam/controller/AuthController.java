package com.neobrutalism.crm.iam.controller;

import com.neobrutalism.crm.iam.service.PermissionService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Authentication Controller
 *
 * Handles authentication operations:
 * - Token validation (called by Gateway for JWT validation)
 * - User permission loading (called by Gateway after token validation)
 * - Token refresh (proxies to Keycloak)
 *
 * All endpoints are reactive for high performance under load
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final PermissionService permissionService;

    public AuthController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Validate JWT token and return user context
     *
     * Called by Gateway after successful JWT signature validation
     * Returns user permissions for caching in Gateway
     *
     * Performance target: < 50ms (with DB query), < 5ms (with cache)
     *
     * @param jwt JWT token (injected by Spring Security)
     * @return UserContext with roles and permissions
     */
    @GetMapping("/validate")
    @Timed(value = "auth.validate", description = "Time to validate token and load permissions")
    public Mono<ResponseEntity<UserContextResponse>> validateToken(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");

        log.debug("Validating token for user {} in tenant {}", userId, tenantId);

        return permissionService.getUserPermissions(userId, tenantId)
                .zipWith(permissionService.getUserRoles(userId, tenantId))
                .map(tuple -> {
                    Map<String, Set<String>> permissions = tuple.getT1();
                    Set<String> roles = tuple.getT2();

                    UserContextResponse response = new UserContextResponse(
                            userId,
                            tenantId,
                            roles,
                            permissions,
                            jwt.getExpiresAt().getEpochSecond()
                    );

                    return ResponseEntity.ok(response);
                })
                .doOnSuccess(response ->
                        log.debug("Token validated successfully for user {}", userId)
                )
                .onErrorResume(error -> {
                    log.error("Error validating token for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get user permissions without full token validation
     *
     * Called by Gateway when token is already validated but permissions need refresh
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Permissions map
     */
    @GetMapping("/permissions/user/{userId}")
    @Timed(value = "auth.get_permissions", description = "Time to get user permissions")
    public Mono<ResponseEntity<PermissionsResponse>> getUserPermissions(
            @PathVariable String userId,
            @RequestParam String tenantId
    ) {
        log.debug("Getting permissions for user {} in tenant {}", userId, tenantId);

        return permissionService.getUserPermissions(userId, tenantId)
                .map(permissions -> {
                    PermissionsResponse response = new PermissionsResponse(
                            userId,
                            tenantId,
                            permissions
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error getting permissions for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Check specific permission
     *
     * Used for explicit permission checks
     *
     * @param request Permission check request
     * @return true if allowed, false otherwise
     */
    @PostMapping("/check-permission")
    @Timed(value = "auth.check_permission", description = "Time to check specific permission")
    public Mono<ResponseEntity<PermissionCheckResponse>> checkPermission(
            @RequestBody PermissionCheckRequest request
    ) {
        log.trace("Checking permission: user={}, tenant={}, resource={}, action={}",
                request.userId, request.tenantId, request.resource, request.action);

        return permissionService.checkPermission(
                        request.userId,
                        request.tenantId,
                        request.resource,
                        request.action
                )
                .map(allowed -> {
                    PermissionCheckResponse response = new PermissionCheckResponse(allowed);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error checking permission: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok(new PermissionCheckResponse(false)));
                });
    }

    /**
     * Get user roles
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Set of role names
     */
    @GetMapping("/roles/user/{userId}")
    @Timed(value = "auth.get_roles", description = "Time to get user roles")
    public Mono<ResponseEntity<RolesResponse>> getUserRoles(
            @PathVariable String userId,
            @RequestParam String tenantId
    ) {
        log.debug("Getting roles for user {} in tenant {}", userId, tenantId);

        return permissionService.getUserRoles(userId, tenantId)
                .map(roles -> {
                    RolesResponse response = new RolesResponse(userId, tenantId, roles);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error getting roles for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== DTOs ====================

    /**
     * User Context Response
     * Contains full user authorization context for caching
     */
    public record UserContextResponse(
            String userId,
            String tenantId,
            Set<String> roles,
            Map<String, Set<String>> permissions,
            long expiresAt
    ) {
    }

    /**
     * Permissions Response
     */
    public record PermissionsResponse(
            String userId,
            String tenantId,
            Map<String, Set<String>> permissions
    ) {
    }

    /**
     * Permission Check Request
     */
    public record PermissionCheckRequest(
            String userId,
            String tenantId,
            String resource,
            String action
    ) {
    }

    /**
     * Permission Check Response
     */
    public record PermissionCheckResponse(
            boolean allowed
    ) {
    }

    /**
     * Roles Response
     */
    public record RolesResponse(
            String userId,
            String tenantId,
            Set<String> roles
    ) {
    }
}
