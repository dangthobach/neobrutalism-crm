package com.neobrutalism.crm.domain.permission.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.permission.dto.PermissionMatrixDTO;
import com.neobrutalism.crm.domain.permission.service.PermissionMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST API for Permission Matrix
 *
 * Provides endpoints for viewing and managing permissions in a matrix format.
 * The matrix shows roles as rows and resources as columns, with permissions as cells.
 *
 * @author Neobrutalism CRM Team
 */
@Slf4j
@RestController
@RequestMapping("/api/permission-matrix")
@RequiredArgsConstructor
@Tag(name = "Permission Matrix", description = "Permission matrix management APIs")
public class PermissionMatrixController {

    private final PermissionMatrixService matrixService;

    /**
     * Get permission matrix for a tenant
     * 
     * @param tenantId Tenant/Organization ID
     * @return Permission matrix DTO
     */
    @GetMapping("/{tenantId}")
    @Operation(summary = "Get permission matrix", 
               description = "Retrieve the permission matrix for a tenant showing roles, resources, and permissions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<ApiResponse<PermissionMatrixDTO>> getPermissionMatrix(
            @PathVariable String tenantId) {
        
        log.debug("Getting permission matrix for tenant: {}", tenantId);
        PermissionMatrixDTO matrix = matrixService.getPermissionMatrix(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Permission matrix retrieved", matrix));
    }

    /**
     * Update permissions in bulk
     * 
     * @param bulkUpdate Bulk update request
     * @return Number of permissions updated
     */
    @PostMapping("/bulk-update")
    @Operation(summary = "Bulk update permissions", 
               description = "Update multiple permissions at once. This will replace existing permissions for the specified role/resource combinations.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> updatePermissionsBulk(
            @Valid @RequestBody PermissionMatrixDTO.BulkPermissionUpdate bulkUpdate) {
        
        log.info("Bulk updating permissions: tenant={}, updates={}", 
                bulkUpdate.getTenantId(), bulkUpdate.getUpdates().size());
        
        int updatedCount = matrixService.updatePermissionsBulk(bulkUpdate);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Updated %d permissions", updatedCount), 
                updatedCount));
    }

    /**
     * Update a single permission cell
     * 
     * @param tenantId Tenant ID
     * @param update Permission update request
     * @return Success status
     */
    @PutMapping("/{tenantId}/permission")
    @Operation(summary = "Update single permission", 
               description = "Update permissions for a specific role/resource combination")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> updatePermission(
            @PathVariable String tenantId,
            @Valid @RequestBody PermissionMatrixDTO.PermissionUpdate update) {
        
        log.debug("Updating permission: tenant={}, role={}, resource={}", 
                tenantId, update.getRoleCode(), update.getResource());
        
        boolean success = matrixService.updatePermission(tenantId, update);
        return ResponseEntity.ok(ApiResponse.success(
                success ? "Permission updated successfully" : "Failed to update permission",
                success));
    }

    /**
     * Refresh permission matrix cache
     * 
     * @param tenantId Tenant ID
     * @return Success status
     */
    @PostMapping("/{tenantId}/refresh")
    @Operation(summary = "Refresh permission matrix cache", 
               description = "Invalidate and refresh the cached permission matrix for a tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> refreshMatrix(
            @PathVariable String tenantId) {
        
        log.info("Refreshing permission matrix cache for tenant: {}", tenantId);
        
        // Force cache refresh by getting the matrix again
        // The cache will be evicted and rebuilt
        PermissionMatrixDTO matrix = matrixService.getPermissionMatrix(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Permission matrix cache refreshed", 
                matrix != null));
    }
}

