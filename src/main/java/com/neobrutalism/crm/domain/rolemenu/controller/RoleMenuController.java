package com.neobrutalism.crm.domain.rolemenu.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.rolemenu.dto.RoleMenuRequest;
import com.neobrutalism.crm.domain.rolemenu.dto.RoleMenuResponse;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import com.neobrutalism.crm.domain.rolemenu.service.RoleMenuExportService;
import com.neobrutalism.crm.domain.rolemenu.service.RoleMenuService;
import com.neobrutalism.crm.domain.rolemenu.service.RoleMenuValidationService;
import com.neobrutalism.crm.utils.exception.ExcelProcessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
    private final RoleMenuValidationService validationService;
    private final RoleMenuExportService exportService;

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

    @PostMapping("/validate")
    @Operation(summary = "Validate permission settings", description = "Validates RoleMenu permission settings for dependencies and conflicts before saving")
    public ApiResponse<Map<String, Object>> validatePermissions(@Valid @RequestBody RoleMenuRequest request) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(request.getRoleId());
        roleMenu.setMenuId(request.getMenuId());
        roleMenu.setCanView(request.getCanView());
        roleMenu.setCanCreate(request.getCanCreate());
        roleMenu.setCanEdit(request.getCanEdit());
        roleMenu.setCanDelete(request.getCanDelete());
        roleMenu.setCanExport(request.getCanExport());
        roleMenu.setCanImport(request.getCanImport());

        RoleMenuValidationService.ValidationResult result = validationService.validate(roleMenu);
        List<String> suggestions = validationService.getSuggestions(result);

        Map<String, Object> response = result.toMap();
        response.put("suggestions", suggestions);

        String message = result.isValid()
            ? "Validation passed"
            : "Validation failed with " + result.getErrors().size() + " error(s)";

        return ApiResponse.success(message, response);
    }

    @PostMapping("/auto-fix")
    @Operation(summary = "Auto-fix permission dependencies", description = "Automatically enables required dependent permissions")
    public ApiResponse<RoleMenuResponse> autoFixPermissions(@Valid @RequestBody RoleMenuRequest request) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(request.getRoleId());
        roleMenu.setMenuId(request.getMenuId());
        roleMenu.setCanView(request.getCanView());
        roleMenu.setCanCreate(request.getCanCreate());
        roleMenu.setCanEdit(request.getCanEdit());
        roleMenu.setCanDelete(request.getCanDelete());
        roleMenu.setCanExport(request.getCanExport());
        roleMenu.setCanImport(request.getCanImport());

        RoleMenu fixed = validationService.autoFixDependencies(roleMenu);

        RoleMenuResponse response = RoleMenuResponse.builder()
            .id(fixed.getId())
            .roleId(fixed.getRoleId())
            .menuId(fixed.getMenuId())
            .canView(fixed.getCanView())
            .canCreate(fixed.getCanCreate())
            .canEdit(fixed.getCanEdit())
            .canDelete(fixed.getCanDelete())
            .canExport(fixed.getCanExport())
            .canImport(fixed.getCanImport())
            .build();

        return ApiResponse.success("Permissions auto-fixed with dependencies", response);
    }

    @GetMapping("/role/{roleId}/export")
    @Operation(summary = "Export role permissions to Excel", description = "Export all menu permissions for a specific role to Excel file")
    public ResponseEntity<byte[]> exportRolePermissions(@PathVariable UUID roleId) throws ExcelProcessException {
        byte[] excelBytes = exportService.exportRolePermissions(roleId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("role_permissions_%s_%s.xlsx", roleId, timestamp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/export-multiple")
    @Operation(summary = "Export multiple roles permissions to Excel", description = "Export menu permissions for multiple roles to a single Excel file")
    public ResponseEntity<byte[]> exportMultipleRolePermissions(@RequestBody List<UUID> roleIds) throws ExcelProcessException {
        byte[] excelBytes = exportService.exportMultipleRolePermissions(roleIds);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("role_permissions_multiple_%s.xlsx", timestamp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export-all")
    @Operation(summary = "Export all role permissions to Excel", description = "Export menu permissions for all roles to Excel file")
    public ResponseEntity<byte[]> exportAllRolePermissions() throws ExcelProcessException {
        byte[] excelBytes = exportService.exportAllRolePermissions();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("all_role_permissions_%s.xlsx", timestamp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
