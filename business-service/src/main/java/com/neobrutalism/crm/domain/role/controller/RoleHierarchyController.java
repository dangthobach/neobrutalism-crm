package com.neobrutalism.crm.domain.role.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleHierarchy;
import com.neobrutalism.crm.domain.role.service.RoleHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST API for Role Hierarchy Management
 */
@Slf4j
@RestController
@RequestMapping("/api/role-hierarchy")
@RequiredArgsConstructor
public class RoleHierarchyController {

    private final RoleHierarchyService hierarchyService;

    /**
     * Create a hierarchy relationship
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleHierarchy>> createHierarchy(
        @RequestBody CreateHierarchyRequest request
    ) {
        RoleHierarchy hierarchy = hierarchyService.createHierarchy(
            request.getParentRoleId(),
            request.getChildRoleId(),
            request.getTenantId(),
            request.getInheritanceType()
        );

        return ResponseEntity.ok(ApiResponse.success("Hierarchy created successfully", hierarchy));
    }

    /**
     * Remove a hierarchy relationship
     */
    @DeleteMapping("/{parentRoleId}/{childRoleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeHierarchy(
        @PathVariable UUID parentRoleId,
        @PathVariable UUID childRoleId,
        @RequestParam String tenantId
    ) {
        hierarchyService.removeHierarchy(parentRoleId, childRoleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Hierarchy removed successfully"));
    }

    /**
     * Get all ancestors (parent roles) for a role
     */
    @GetMapping("/{roleId}/ancestors")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Role>>> getAncestors(
        @PathVariable UUID roleId,
        @RequestParam String tenantId
    ) {
        List<Role> ancestors = hierarchyService.getAncestors(roleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Ancestors retrieved successfully", ancestors));
    }

    /**
     * Get all descendants (child roles) for a role
     */
    @GetMapping("/{roleId}/descendants")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Role>>> getDescendants(
        @PathVariable UUID roleId,
        @RequestParam String tenantId
    ) {
        List<Role> descendants = hierarchyService.getDescendants(roleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Descendants retrieved successfully", descendants));
    }

    /**
     * Get direct parents for a role
     */
    @GetMapping("/{roleId}/parents")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleHierarchy>>> getDirectParents(
        @PathVariable UUID roleId,
        @RequestParam String tenantId
    ) {
        List<RoleHierarchy> parents = hierarchyService.getDirectParents(roleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Direct parents retrieved successfully", parents));
    }

    /**
     * Get direct children for a role
     */
    @GetMapping("/{roleId}/children")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleHierarchy>>> getDirectChildren(
        @PathVariable UUID roleId,
        @RequestParam String tenantId
    ) {
        List<RoleHierarchy> children = hierarchyService.getDirectChildren(roleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Direct children retrieved successfully", children));
    }

    /**
     * Get all inherited role codes for a list of role codes
     */
    @PostMapping("/inherited-roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Set<String>>> getInheritedRoles(
        @RequestBody InheritedRolesRequest request
    ) {
        Set<String> inheritedRoles = hierarchyService.getInheritedRoleCodes(
            request.getRoleCodes(),
            request.getTenantId()
        );
        return ResponseEntity.ok(ApiResponse.success("Inherited roles retrieved successfully", inheritedRoles));
    }

    /**
     * Get the complete hierarchy tree for a tenant
     */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleHierarchy>>> getHierarchyTree(
        @RequestParam String tenantId
    ) {
        List<RoleHierarchy> tree = hierarchyService.getHierarchyTree(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Hierarchy tree retrieved successfully", tree));
    }

    /**
     * Get visual hierarchy tree structure
     */
    @GetMapping("/tree/visual")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVisualHierarchyTree(
        @RequestParam String tenantId
    ) {
        Map<String, Object> tree = hierarchyService.buildHierarchyTree(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Visual hierarchy tree retrieved successfully", tree));
    }

    /**
     * Check if one role is an ancestor of another
     */
    @GetMapping("/is-ancestor")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Boolean>> isAncestor(
        @RequestParam UUID ancestorRoleId,
        @RequestParam UUID descendantRoleId,
        @RequestParam String tenantId
    ) {
        boolean isAncestor = hierarchyService.isAncestor(ancestorRoleId, descendantRoleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Ancestor check completed", isAncestor));
    }

    /**
     * Get hierarchy depth for a role
     */
    @GetMapping("/{roleId}/depth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> getHierarchyDepth(
        @PathVariable UUID roleId,
        @RequestParam String tenantId
    ) {
        int depth = hierarchyService.getHierarchyDepth(roleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Hierarchy depth retrieved successfully", depth));
    }

    /**
     * Activate a hierarchy relationship
     */
    @PutMapping("/{parentRoleId}/{childRoleId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateHierarchy(
        @PathVariable UUID parentRoleId,
        @PathVariable UUID childRoleId,
        @RequestParam String tenantId
    ) {
        hierarchyService.activateHierarchy(parentRoleId, childRoleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Hierarchy activated successfully"));
    }

    /**
     * Deactivate a hierarchy relationship
     */
    @PutMapping("/{parentRoleId}/{childRoleId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateHierarchy(
        @PathVariable UUID parentRoleId,
        @PathVariable UUID childRoleId,
        @RequestParam String tenantId
    ) {
        hierarchyService.deactivateHierarchy(parentRoleId, childRoleId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Hierarchy deactivated successfully"));
    }

    /**
     * Request DTOs
     */
    @lombok.Data
    public static class CreateHierarchyRequest {
        private UUID parentRoleId;
        private UUID childRoleId;
        private String tenantId;
        private RoleHierarchy.InheritanceType inheritanceType = RoleHierarchy.InheritanceType.FULL;
    }

    @lombok.Data
    public static class InheritedRolesRequest {
        private List<String> roleCodes;
        private String tenantId;
    }
}
