package com.neobrutalism.crm.iam.controller;

import com.neobrutalism.crm.iam.service.PermissionService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Permission Management Controller
 *
 * Admin endpoints for managing roles and permissions
 * Requires ROLE_SUPER_ADMIN or ROLE_TENANT_ADMIN
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private static final Logger log = LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Add a role to a user
     *
     * @param request Add role request
     * @return Success status
     */
    @PostMapping("/roles/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Timed(value = "permissions.assign_role", description = "Time to assign role to user")
    public Mono<ResponseEntity<OperationResponse>> addRoleToUser(
            @Valid @RequestBody AddRoleRequest request
    ) {
        log.info("Adding role {} to user {} in tenant {}", request.roleId, request.userId, request.tenantId);

        return permissionService.addRoleToUser(request.userId, request.roleId, request.tenantId)
                .map(result -> {
                    if (result) {
                        return ResponseEntity.ok(new OperationResponse(true, "Role assigned successfully"));
                    } else {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new OperationResponse(false, "Role assignment failed - already exists"));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error adding role to user: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new OperationResponse(false, "Internal error: " + error.getMessage())));
                });
    }

    /**
     * Remove a role from a user
     *
     * @param request Remove role request
     * @return Success status
     */
    @PostMapping("/roles/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Timed(value = "permissions.revoke_role", description = "Time to revoke role from user")
    public Mono<ResponseEntity<OperationResponse>> removeRoleFromUser(
            @Valid @RequestBody RemoveRoleRequest request
    ) {
        log.info("Removing role {} from user {} in tenant {}", request.roleId, request.userId, request.tenantId);

        return permissionService.removeRoleFromUser(request.userId, request.roleId, request.tenantId)
                .map(result -> {
                    if (result) {
                        return ResponseEntity.ok(new OperationResponse(true, "Role revoked successfully"));
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new OperationResponse(false, "Role revocation failed - not found"));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error removing role from user: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new OperationResponse(false, "Internal error: " + error.getMessage())));
                });
    }

    /**
     * Add a permission policy
     *
     * @param request Add policy request
     * @return Success status
     */
    @PostMapping("/policies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Timed(value = "permissions.add_policy", description = "Time to add policy")
    public Mono<ResponseEntity<OperationResponse>> addPolicy(
            @Valid @RequestBody AddPolicyRequest request
    ) {
        log.info("Adding policy: role={}, tenant={}, resource={}, action={}",
                request.roleId, request.tenantId, request.resource, request.action);

        return permissionService.addPolicy(request.roleId, request.tenantId, request.resource, request.action)
                .map(result -> {
                    if (result) {
                        return ResponseEntity.ok(new OperationResponse(true, "Policy added successfully"));
                    } else {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new OperationResponse(false, "Policy addition failed - already exists"));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error adding policy: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new OperationResponse(false, "Internal error: " + error.getMessage())));
                });
    }

    /**
     * Remove a permission policy
     *
     * @param request Remove policy request
     * @return Success status
     */
    @DeleteMapping("/policies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Timed(value = "permissions.remove_policy", description = "Time to remove policy")
    public Mono<ResponseEntity<OperationResponse>> removePolicy(
            @Valid @RequestBody RemovePolicyRequest request
    ) {
        log.info("Removing policy: role={}, tenant={}, resource={}, action={}",
                request.roleId, request.tenantId, request.resource, request.action);

        return permissionService.removePolicy(request.roleId, request.tenantId, request.resource, request.action)
                .map(result -> {
                    if (result) {
                        return ResponseEntity.ok(new OperationResponse(true, "Policy removed successfully"));
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new OperationResponse(false, "Policy removal failed - not found"));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error removing policy: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new OperationResponse(false, "Internal error: " + error.getMessage())));
                });
    }

    // ==================== DTOs ====================

    /**
     * Add Role Request
     */
    public record AddRoleRequest(
            @NotBlank(message = "userId is required")
            String userId,

            @NotBlank(message = "roleId is required")
            String roleId,

            @NotBlank(message = "tenantId is required")
            String tenantId
    ) {
    }

    /**
     * Remove Role Request
     */
    public record RemoveRoleRequest(
            @NotBlank(message = "userId is required")
            String userId,

            @NotBlank(message = "roleId is required")
            String roleId,

            @NotBlank(message = "tenantId is required")
            String tenantId
    ) {
    }

    /**
     * Add Policy Request
     */
    public record AddPolicyRequest(
            @NotBlank(message = "roleId is required")
            String roleId,

            @NotBlank(message = "tenantId is required")
            String tenantId,

            @NotBlank(message = "resource is required")
            String resource,

            @NotBlank(message = "action is required")
            String action
    ) {
    }

    /**
     * Remove Policy Request
     */
    public record RemovePolicyRequest(
            @NotBlank(message = "roleId is required")
            String roleId,

            @NotBlank(message = "tenantId is required")
            String tenantId,

            @NotBlank(message = "resource is required")
            String resource,

            @NotBlank(message = "action is required")
            String action
    ) {
    }

    /**
     * Operation Response
     */
    public record OperationResponse(
            boolean success,
            String message
    ) {
    }
}
