package com.neobrutalism.crm.domain.userrole.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.userrole.dto.UserRoleRequest;
import com.neobrutalism.crm.domain.userrole.dto.UserRoleResponse;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import com.neobrutalism.crm.domain.userrole.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for UserRole management (User-to-Role assignments)
 */
@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
@Tag(name = "User Roles", description = "User role assignment APIs")
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get roles by user", description = "Retrieve all roles assigned to a specific user")
    public ApiResponse<List<UserRoleResponse>> getRolesByUser(@PathVariable UUID userId) {
        List<UserRole> userRoles = userRoleService.findByUserId(userId);
        List<UserRoleResponse> responses = userRoles.stream()
                .map(UserRoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active roles by user", description = "Retrieve all active roles assigned to a specific user")
    public ApiResponse<List<UserRoleResponse>> getActiveRolesByUser(@PathVariable UUID userId) {
        List<UserRole> userRoles = userRoleService.findActiveByUserId(userId);
        List<UserRoleResponse> responses = userRoles.stream()
                .map(UserRoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "Get users by role", description = "Retrieve all users assigned to a specific role")
    public ApiResponse<List<UserRoleResponse>> getUsersByRole(@PathVariable UUID roleId) {
        List<UserRole> userRoles = userRoleService.findByRoleId(roleId);
        List<UserRoleResponse> responses = userRoles.stream()
                .map(UserRoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign role to user", description = "Assign a role to a user")
    public ApiResponse<UserRoleResponse> assignRoleToUser(@Valid @RequestBody UserRoleRequest request) {
        // Check if assignment already exists
        userRoleService.findByUserIdAndRoleId(request.getUserId(), request.getRoleId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("User already has this role assigned");
                });

        UserRole userRole = new UserRole();
        userRole.setUserId(request.getUserId());
        userRole.setRoleId(request.getRoleId());
        userRole.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        userRole.setGrantedAt(Instant.now());
        userRole.setExpiresAt(request.getExpiresAt());

        UserRole created = userRoleService.create(userRole);
        return ApiResponse.success("Role assigned to user successfully", UserRoleResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user role assignment", description = "Update a user role assignment (e.g., activate/deactivate, change expiration)")
    public ApiResponse<UserRoleResponse> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserRoleRequest request) {

        UserRole userRole = userRoleService.findById(id);
        userRole.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        userRole.setExpiresAt(request.getExpiresAt());

        UserRole updated = userRoleService.update(id, userRole);
        return ApiResponse.success("User role updated successfully", UserRoleResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke role from user", description = "Remove a role assignment from a user")
    public ApiResponse<Void> revokeRoleFromUser(@PathVariable UUID id) {
        userRoleService.deleteById(id);
        return ApiResponse.success("Role revoked from user successfully");
    }

    @DeleteMapping("/user/{userId}/role/{roleId}")
    @Operation(summary = "Revoke specific role from user", description = "Remove a specific role from a user by user ID and role ID")
    public ApiResponse<Void> revokeSpecificRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        userRoleService.removeUserRole(userId, roleId);
        return ApiResponse.success("Role revoked from user successfully");
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user role", description = "Activate a user role assignment")
    public ApiResponse<UserRoleResponse> activateUserRole(@PathVariable UUID id) {
        UserRole userRole = userRoleService.findById(id);
        userRole.setIsActive(true);
        UserRole updated = userRoleService.update(id, userRole);
        return ApiResponse.success("User role activated", UserRoleResponse.from(updated));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user role", description = "Deactivate a user role assignment")
    public ApiResponse<UserRoleResponse> deactivateUserRole(@PathVariable UUID id) {
        UserRole userRole = userRoleService.findById(id);
        userRole.setIsActive(false);
        UserRole updated = userRoleService.update(id, userRole);
        return ApiResponse.success("User role deactivated", UserRoleResponse.from(updated));
    }

    @PostMapping("/expire-expired")
    @Operation(summary = "Expire expired roles", description = "Manually trigger expiration of all roles that have passed their expiration date")
    public ApiResponse<Void> expireExpiredRoles() {
        userRoleService.expireExpiredRoles();
        return ApiResponse.success("Expired roles have been deactivated");
    }
}
