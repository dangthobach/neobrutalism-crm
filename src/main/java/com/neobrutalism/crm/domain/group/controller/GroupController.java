package com.neobrutalism.crm.domain.group.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.util.SortValidator;
import com.neobrutalism.crm.domain.group.dto.GroupRequest;
import com.neobrutalism.crm.domain.group.dto.GroupResponse;
import com.neobrutalism.crm.domain.group.model.Group;
import com.neobrutalism.crm.domain.group.model.GroupStatus;
import com.neobrutalism.crm.domain.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Group management
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management APIs")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    @Operation(summary = "Get all groups", description = "Retrieve all active groups with pagination")
    public ApiResponse<PageResponse<GroupResponse>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        String validatedSortBy = SortValidator.validateGroupSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<Group> groupPage = groupService.findAllActive(pageable);
        Page<GroupResponse> responsePage = groupPage.map(GroupResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID", description = "Retrieve a specific group by its ID")
    public ApiResponse<GroupResponse> getGroupById(@PathVariable UUID id) {
        Group group = groupService.findById(id);
        return ApiResponse.success(GroupResponse.from(group));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get group by code", description = "Retrieve a specific group by its unique code")
    public ApiResponse<GroupResponse> getGroupByCode(@PathVariable String code) {
        Group group = groupService.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("Group", "code", code));
        return ApiResponse.success(GroupResponse.from(group));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get groups by organization", description = "Retrieve all groups belonging to a specific organization")
    public ApiResponse<List<GroupResponse>> getGroupsByOrganization(@PathVariable UUID organizationId) {
        List<Group> groups = groupService.findByOrganizationId(organizationId);
        List<GroupResponse> responses = groups.stream()
                .map(GroupResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "Get child groups", description = "Retrieve all groups that are children of a specific parent group")
    public ApiResponse<List<GroupResponse>> getChildGroups(@PathVariable UUID parentId) {
        List<Group> groups = groupService.findByParentId(parentId);
        List<GroupResponse> responses = groups.stream()
                .map(GroupResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/root")
    @Operation(summary = "Get root groups", description = "Retrieve all top-level groups (groups without parent)")
    public ApiResponse<List<GroupResponse>> getRootGroups() {
        List<Group> groups = groupService.findRootGroups();
        List<GroupResponse> responses = groups.stream()
                .map(GroupResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create group", description = "Create a new group")
    public ApiResponse<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        Group group = new Group();
        group.setCode(request.getCode());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setParentId(request.getParentId());
        group.setOrganizationId(request.getOrganizationId());
        group.setTenantId(request.getOrganizationId().toString()); // Set tenant ID same as organization

        // Calculate level and path based on parent
        if (request.getParentId() != null) {
            Group parent = groupService.findById(request.getParentId());
            group.setLevel(parent.getLevel() + 1);
            group.setPath(parent.getPath() + "/" + request.getCode());
        } else {
            group.setLevel(0);
            group.setPath("/" + request.getCode());
        }

        Group created = groupService.create(group);
        return ApiResponse.success("Group created successfully", GroupResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update group", description = "Update an existing group")
    public ApiResponse<GroupResponse> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody GroupRequest request) {

        Group group = groupService.findById(id);
        group.setCode(request.getCode());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setOrganizationId(request.getOrganizationId());

        // Handle parent change - recalculate level and path
        if (request.getParentId() != null) {
            // Prevent circular reference
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Group cannot be its own parent");
            }

            Group parent = groupService.findById(request.getParentId());

            // Prevent setting a child as parent
            if (parent.getPath() != null && parent.getPath().contains(group.getPath())) {
                throw new IllegalArgumentException("Cannot set a descendant as parent");
            }

            group.setParentId(request.getParentId());
            group.setLevel(parent.getLevel() + 1);
            group.setPath(parent.getPath() + "/" + request.getCode());
        } else {
            group.setParentId(null);
            group.setLevel(0);
            group.setPath("/" + request.getCode());
        }

        Group updated = groupService.update(id, group);
        return ApiResponse.success("Group updated successfully", GroupResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group", description = "Soft delete a group")
    public ApiResponse<Void> deleteGroup(@PathVariable UUID id) {
        // Check if group has children
        List<Group> children = groupService.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete group with children. Please delete or reassign child groups first.");
        }

        groupService.deleteById(id);
        return ApiResponse.success("Group deleted successfully");
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate group", description = "Change group status to ACTIVE")
    public ApiResponse<GroupResponse> activateGroup(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Group activated = groupService.activate(id, reason);
        return ApiResponse.success("Group activated successfully", GroupResponse.from(activated));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate group", description = "Change group status to INACTIVE")
    public ApiResponse<GroupResponse> deactivateGroup(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Group deactivated = groupService.transitionTo(id, GroupStatus.INACTIVE, reason);
        return ApiResponse.success("Group deactivated successfully", GroupResponse.from(deactivated));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get groups by status", description = "Retrieve all groups with a specific status")
    public ApiResponse<List<GroupResponse>> getGroupsByStatus(@PathVariable GroupStatus status) {
        List<Group> groups = groupService.findByStatus(status);
        List<GroupResponse> responses = groups.stream()
                .map(GroupResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
