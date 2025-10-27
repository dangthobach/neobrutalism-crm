package com.neobrutalism.crm.domain.grouprole.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.grouprole.dto.GroupRoleRequest;
import com.neobrutalism.crm.domain.grouprole.dto.GroupRoleResponse;
import com.neobrutalism.crm.domain.grouprole.model.GroupRole;
import com.neobrutalism.crm.domain.grouprole.service.GroupRoleService;
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
 * REST controller for GroupRole management (Group-to-Role assignments)
 */
@RestController
@RequestMapping("/api/group-roles")
@RequiredArgsConstructor
@Tag(name = "Group Roles", description = "Group role assignment APIs")
public class GroupRoleController {

    private final GroupRoleService groupRoleService;

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get roles by group", description = "Retrieve all roles assigned to a specific group")
    public ApiResponse<List<GroupRoleResponse>> getRolesByGroup(@PathVariable UUID groupId) {
        List<GroupRole> groupRoles = groupRoleService.findByGroupId(groupId);
        List<GroupRoleResponse> responses = groupRoles.stream()
                .map(GroupRoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "Get groups by role", description = "Retrieve all groups assigned to a specific role")
    public ApiResponse<List<GroupRoleResponse>> getGroupsByRole(@PathVariable UUID roleId) {
        List<GroupRole> groupRoles = groupRoleService.findByRoleId(roleId);
        List<GroupRoleResponse> responses = groupRoles.stream()
                .map(GroupRoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign role to group", description = "Assign a role to a group")
    public ApiResponse<GroupRoleResponse> assignRoleToGroup(@Valid @RequestBody GroupRoleRequest request) {
        // Check if assignment already exists
        groupRoleService.findByGroupIdAndRoleId(request.getGroupId(), request.getRoleId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Group already has this role assigned");
                });

        GroupRole groupRole = new GroupRole();
        groupRole.setGroupId(request.getGroupId());
        groupRole.setRoleId(request.getRoleId());
        groupRole.setGrantedAt(Instant.now());

        GroupRole created = groupRoleService.create(groupRole);
        return ApiResponse.success("Role assigned to group successfully", GroupRoleResponse.from(created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke role from group", description = "Remove a role assignment from a group")
    public ApiResponse<Void> revokeRoleFromGroup(@PathVariable UUID id) {
        groupRoleService.deleteById(id);
        return ApiResponse.success("Role revoked from group successfully");
    }

    @DeleteMapping("/group/{groupId}/role/{roleId}")
    @Operation(summary = "Revoke specific role from group", description = "Remove a specific role from a group by group ID and role ID")
    public ApiResponse<Void> revokeSpecificRoleFromGroup(
            @PathVariable UUID groupId,
            @PathVariable UUID roleId) {
        groupRoleService.removeGroupRole(groupId, roleId);
        return ApiResponse.success("Role revoked from group successfully");
    }
}
