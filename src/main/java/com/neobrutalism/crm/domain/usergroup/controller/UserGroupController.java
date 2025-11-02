package com.neobrutalism.crm.domain.usergroup.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.usergroup.dto.UserGroupRequest;
import com.neobrutalism.crm.domain.usergroup.dto.UserGroupResponse;
import com.neobrutalism.crm.domain.usergroup.model.UserGroup;
import com.neobrutalism.crm.domain.usergroup.service.UserGroupService;
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
 * REST controller for UserGroup management (User-to-Group assignments)
 */
@RestController
@RequestMapping("/api/user-groups")
@RequiredArgsConstructor
@Tag(name = "User Groups", description = "User group assignment APIs")
public class UserGroupController {

    private final UserGroupService userGroupService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get groups by user", description = "Retrieve all groups assigned to a specific user")
    public ApiResponse<List<UserGroupResponse>> getGroupsByUser(@PathVariable UUID userId) {
        List<UserGroup> userGroups = userGroupService.findByUserId(userId);
        List<UserGroupResponse> responses = userGroups.stream()
                .map(UserGroupResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get users by group", description = "Retrieve all users assigned to a specific group")
    public ApiResponse<List<UserGroupResponse>> getUsersByGroup(@PathVariable UUID groupId) {
        List<UserGroup> userGroups = userGroupService.findByGroupId(groupId);
        List<UserGroupResponse> responses = userGroups.stream()
                .map(UserGroupResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/user/{userId}/primary")
    @Operation(summary = "Get user's primary group", description = "Retrieve the primary group for a specific user")
    public ApiResponse<UserGroupResponse> getPrimaryGroup(@PathVariable UUID userId) {
        UserGroup userGroup = userGroupService.findPrimaryGroup(userId)
                .orElseThrow(() -> new IllegalStateException("User has no primary group"));
        return ApiResponse.success(UserGroupResponse.from(userGroup));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign user to group", description = "Assign a user to a group")
    public ApiResponse<UserGroupResponse> assignUserToGroup(@Valid @RequestBody UserGroupRequest request) {
        // Check if assignment already exists
        userGroupService.findByUserIdAndGroupId(request.getUserId(), request.getGroupId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("User is already assigned to this group");
                });

        UserGroup userGroup = new UserGroup();
        userGroup.setUserId(request.getUserId());
        userGroup.setGroupId(request.getGroupId());
        userGroup.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
        userGroup.setJoinedAt(Instant.now());

        UserGroup created = userGroupService.create(userGroup);
        return ApiResponse.success("User assigned to group successfully", UserGroupResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user group assignment", description = "Update a user group assignment (e.g., set as primary)")
    public ApiResponse<UserGroupResponse> updateUserGroup(
            @PathVariable UUID id,
            @Valid @RequestBody UserGroupRequest request) {

        UserGroup userGroup = userGroupService.findById(id);
        userGroup.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);

        UserGroup updated = userGroupService.update(id, userGroup);
        return ApiResponse.success("User group assignment updated successfully", UserGroupResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove user from group", description = "Remove a user from a group by assignment ID")
    public ApiResponse<Void> removeUserFromGroup(@PathVariable UUID id) {
        userGroupService.deleteById(id);
        return ApiResponse.success("User removed from group successfully");
    }

    @DeleteMapping("/user/{userId}/group/{groupId}")
    @Operation(summary = "Remove specific user from group", description = "Remove a specific user from a group by user ID and group ID")
    public ApiResponse<Void> removeSpecificUserFromGroup(
            @PathVariable UUID userId,
            @PathVariable UUID groupId) {
        userGroupService.removeUserFromGroup(userId, groupId);
        return ApiResponse.success("User removed from group successfully");
    }

    @PostMapping("/{id}/set-primary")
    @Operation(summary = "Set as primary group", description = "Set this group as the user's primary group")
    public ApiResponse<UserGroupResponse> setPrimaryGroup(@PathVariable UUID id) {
        UserGroup userGroup = userGroupService.findById(id);
        UUID userId = userGroup.getUserId();

        // Unset current primary group if exists
        userGroupService.findPrimaryGroup(userId).ifPresent(currentPrimary -> {
            if (!currentPrimary.getId().equals(id)) {
                currentPrimary.setIsPrimary(false);
                userGroupService.update(currentPrimary.getId(), currentPrimary);
            }
        });

        // Set new primary group
        userGroup.setIsPrimary(true);
        UserGroup updated = userGroupService.update(id, userGroup);
        return ApiResponse.success("Primary group set successfully", UserGroupResponse.from(updated));
    }
}
