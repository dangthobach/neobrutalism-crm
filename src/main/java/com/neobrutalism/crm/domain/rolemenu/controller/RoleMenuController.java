package com.neobrutalism.crm.domain.rolemenu.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.rolemenu.dto.RoleMenuRequest;
import com.neobrutalism.crm.domain.rolemenu.dto.RoleMenuResponse;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.service.RoleMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for RoleMenu management (Role-to-Menu permission assignments)
 */
@RestController
@RequestMapping("/api/role-menus")
@RequiredArgsConstructor
@Tag(name = "Role Menus", description = "Role menu permission APIs")
public class RoleMenuController {

    private final RoleMenuService roleMenuService;

    @GetMapping("/role/{roleId}")
    @Operation(summary = "Get menu permissions by role", description = "Retrieve all menu permissions for a specific role")
    public ApiResponse<List<RoleMenuResponse>> getMenuPermissionsByRole(@PathVariable UUID roleId) {
        List<RoleMenu> roleMenus = roleMenuService.findByRoleId(roleId);
        List<RoleMenuResponse> responses = roleMenus.stream()
                .map(RoleMenuResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/menu/{menuId}")
    @Operation(summary = "Get role permissions by menu", description = "Retrieve all role permissions for a specific menu")
    public ApiResponse<List<RoleMenuResponse>> getRolePermissionsByMenu(@PathVariable UUID menuId) {
        List<RoleMenu> roleMenus = roleMenuService.findByMenuId(menuId);
        List<RoleMenuResponse> responses = roleMenus.stream()
                .map(RoleMenuResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Set menu permissions for role", description = "Create menu permission assignment for a role")
    public ApiResponse<RoleMenuResponse> setMenuPermissions(@Valid @RequestBody RoleMenuRequest request) {
        // Check if permission already exists
        roleMenuService.findByRoleIdAndMenuId(request.getRoleId(), request.getMenuId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Menu permission for this role already exists");
                });

        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(request.getRoleId());
        roleMenu.setMenuId(request.getMenuId());
        roleMenu.setCanView(request.getCanView() != null ? request.getCanView() : true);
        roleMenu.setCanCreate(request.getCanCreate() != null ? request.getCanCreate() : false);
        roleMenu.setCanEdit(request.getCanEdit() != null ? request.getCanEdit() : false);
        roleMenu.setCanDelete(request.getCanDelete() != null ? request.getCanDelete() : false);
        roleMenu.setCanExport(request.getCanExport() != null ? request.getCanExport() : false);
        roleMenu.setCanImport(request.getCanImport() != null ? request.getCanImport() : false);

        RoleMenu created = roleMenuService.create(roleMenu);
        return ApiResponse.success("Menu permissions set for role successfully", RoleMenuResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu permissions", description = "Update menu permissions for a role (canView, canCreate, canEdit, canDelete, canExport, canImport)")
    public ApiResponse<RoleMenuResponse> updateMenuPermissions(
            @PathVariable UUID id,
            @Valid @RequestBody RoleMenuRequest request) {

        RoleMenu roleMenu = roleMenuService.findById(id);
        roleMenu.setCanView(request.getCanView() != null ? request.getCanView() : roleMenu.getCanView());
        roleMenu.setCanCreate(request.getCanCreate() != null ? request.getCanCreate() : roleMenu.getCanCreate());
        roleMenu.setCanEdit(request.getCanEdit() != null ? request.getCanEdit() : roleMenu.getCanEdit());
        roleMenu.setCanDelete(request.getCanDelete() != null ? request.getCanDelete() : roleMenu.getCanDelete());
        roleMenu.setCanExport(request.getCanExport() != null ? request.getCanExport() : roleMenu.getCanExport());
        roleMenu.setCanImport(request.getCanImport() != null ? request.getCanImport() : roleMenu.getCanImport());

        RoleMenu updated = roleMenuService.update(id, roleMenu);
        return ApiResponse.success("Menu permissions updated successfully", RoleMenuResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke menu permission", description = "Remove a menu permission from a role")
    public ApiResponse<Void> revokeMenuPermission(@PathVariable UUID id) {
        roleMenuService.deleteById(id);
        return ApiResponse.success("Menu permission revoked successfully");
    }

    @DeleteMapping("/role/{roleId}/menu/{menuId}")
    @Operation(summary = "Revoke specific menu permission", description = "Remove a specific menu permission from a role by role ID and menu ID")
    public ApiResponse<Void> revokeSpecificMenuPermission(
            @PathVariable UUID roleId,
            @PathVariable UUID menuId) {
        roleMenuService.removeRoleMenu(roleId, menuId);
        return ApiResponse.success("Menu permission revoked successfully");
    }

    @PostMapping("/role/{roleId}/copy-from/{sourceRoleId}")
    @Operation(summary = "Copy permissions from another role", description = "Copy all menu permissions from a source role to a target role")
    public ApiResponse<List<RoleMenuResponse>> copyPermissionsFromRole(
            @PathVariable UUID roleId,
            @PathVariable UUID sourceRoleId) {

        if (roleId.equals(sourceRoleId)) {
            throw new IllegalArgumentException("Cannot copy permissions to the same role");
        }

        // Get all permissions from source role
        List<RoleMenu> sourcePermissions = roleMenuService.findByRoleId(sourceRoleId);

        if (sourcePermissions.isEmpty()) {
            throw new IllegalStateException("Source role has no permissions to copy");
        }

        // Copy permissions to target role
        List<RoleMenu> copiedPermissions = sourcePermissions.stream()
                .map(source -> {
                    // Check if permission already exists for target role
                    if (roleMenuService.findByRoleIdAndMenuId(roleId, source.getMenuId()).isPresent()) {
                        return null; // Skip existing permissions
                    }

                    RoleMenu newPermission = new RoleMenu();
                    newPermission.setRoleId(roleId);
                    newPermission.setMenuId(source.getMenuId());
                    newPermission.setCanView(source.getCanView());
                    newPermission.setCanCreate(source.getCanCreate());
                    newPermission.setCanEdit(source.getCanEdit());
                    newPermission.setCanDelete(source.getCanDelete());
                    newPermission.setCanExport(source.getCanExport());
                    newPermission.setCanImport(source.getCanImport());

                    return roleMenuService.create(newPermission);
                })
                .filter(permission -> permission != null)
                .toList();

        List<RoleMenuResponse> responses = copiedPermissions.stream()
                .map(RoleMenuResponse::from)
                .toList();

        return ApiResponse.success(
            String.format("Successfully copied %d permissions from source role", copiedPermissions.size()),
            responses
        );
    }
}
