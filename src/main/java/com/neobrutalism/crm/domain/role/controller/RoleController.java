package com.neobrutalism.crm.domain.role.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.dto.PageResponse;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.util.SortValidator;
import com.neobrutalism.crm.domain.role.dto.RoleRequest;
import com.neobrutalism.crm.domain.role.dto.RoleResponse;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleStatus;
import com.neobrutalism.crm.domain.role.service.RoleService;
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
 * REST controller for Role management
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management APIs")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieve all active roles with pagination")
    public ApiResponse<PageResponse<RoleResponse>> getAllRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        String validatedSortBy = SortValidator.validateRoleSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<Role> rolePage = roleService.findAllActive(pageable);
        Page<RoleResponse> responsePage = rolePage.map(RoleResponse::from);

        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve a specific role by its ID")
    public ApiResponse<RoleResponse> getRoleById(@PathVariable UUID id) {
        Role role = roleService.findById(id);
        return ApiResponse.success(RoleResponse.from(role));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get role by code", description = "Retrieve a specific role by its unique code")
    public ApiResponse<RoleResponse> getRoleByCode(@PathVariable String code) {
        Role role = roleService.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.forResourceByField("Role", "code", code));
        return ApiResponse.success(RoleResponse.from(role));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get roles by organization", description = "Retrieve all roles belonging to a specific organization")
    public ApiResponse<List<RoleResponse>> getRolesByOrganization(@PathVariable UUID organizationId) {
        List<Role> roles = roleService.findByOrganizationId(organizationId);
        List<RoleResponse> responses = roles.stream()
                .map(RoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/system")
    @Operation(summary = "Get system roles", description = "Retrieve all system-defined roles")
    public ApiResponse<List<RoleResponse>> getSystemRoles() {
        List<Role> roles = roleService.findSystemRoles();
        List<RoleResponse> responses = roles.stream()
                .map(RoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create role", description = "Create a new role")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setOrganizationId(request.getOrganizationId());
        role.setIsSystem(request.getIsSystem() != null ? request.getIsSystem() : false);
        role.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        role.setTenantId(request.getOrganizationId().toString()); // Set tenant ID same as organization

        Role created = roleService.create(role);
        return ApiResponse.success("Role created successfully", RoleResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update an existing role")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request) {

        Role role = roleService.findById(id);

        // Prevent modification of system roles
        if (role.getIsSystem() != null && role.getIsSystem()) {
            throw new IllegalStateException("Cannot modify system roles");
        }

        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setOrganizationId(request.getOrganizationId());
        role.setPriority(request.getPriority() != null ? request.getPriority() : 0);

        Role updated = roleService.update(id, role);
        return ApiResponse.success("Role updated successfully", RoleResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Soft delete a role")
    public ApiResponse<Void> deleteRole(@PathVariable UUID id) {
        Role role = roleService.findById(id);

        // Prevent deletion of system roles
        if (role.getIsSystem() != null && role.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system roles");
        }

        roleService.deleteById(id);
        return ApiResponse.success("Role deleted successfully");
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate role", description = "Change role status to ACTIVE")
    public ApiResponse<RoleResponse> activateRole(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Role activated = roleService.activate(id, reason);
        return ApiResponse.success("Role activated successfully", RoleResponse.from(activated));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate role", description = "Change role status to INACTIVE")
    public ApiResponse<RoleResponse> deactivateRole(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {

        Role role = roleService.findById(id);

        // Prevent deactivation of system roles
        if (role.getIsSystem() != null && role.getIsSystem()) {
            throw new IllegalStateException("Cannot deactivate system roles");
        }

        Role deactivated = roleService.transitionTo(id, RoleStatus.INACTIVE, reason);
        return ApiResponse.success("Role deactivated successfully", RoleResponse.from(deactivated));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get roles by status", description = "Retrieve all roles with a specific status")
    public ApiResponse<List<RoleResponse>> getRolesByStatus(@PathVariable RoleStatus status) {
        List<Role> roles = roleService.findByStatus(status);
        List<RoleResponse> responses = roles.stream()
                .map(RoleResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
