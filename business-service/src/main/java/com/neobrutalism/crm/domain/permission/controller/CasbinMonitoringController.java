package com.neobrutalism.crm.domain.permission.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.config.security.CasbinCacheService;
import com.neobrutalism.crm.config.security.CasbinPolicyMonitoringService;
import com.neobrutalism.crm.config.security.RoleHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Casbin Monitoring & Management Controller
 *
 * Purpose: Provide REST API for Casbin monitoring, cache management, and role hierarchy
 *
 * Endpoints:
 * - GET /api/casbin/monitoring/stats - Get monitoring statistics
 * - POST /api/casbin/monitoring/health-check - Trigger manual health check
 * - GET /api/casbin/cache/stats - Get cache statistics
 * - POST /api/casbin/cache/clear - Clear all caches
 * - GET /api/casbin/hierarchy - Get role hierarchy structure
 * - POST /api/casbin/hierarchy/inherit - Add role inheritance
 *
 * Security: Only accessible by ADMIN role
 *
 * @author Neobrutalism CRM Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/casbin")
@RequiredArgsConstructor
@Tag(name = "Casbin Monitoring", description = "Casbin monitoring, cache management, and role hierarchy APIs")
public class CasbinMonitoringController {

    private final CasbinPolicyMonitoringService monitoringService;
    private final CasbinCacheService cacheService;
    private final RoleHierarchyService hierarchyService;

    /**
     * Get policy monitoring statistics
     */
    @GetMapping("/monitoring/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get policy monitoring statistics",
               description = "Returns policy count, alert level, tenant stats, and threshold information")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonitoringStats() {
        log.debug("Getting Casbin monitoring statistics");
        Map<String, Object> stats = monitoringService.getMonitoringStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Trigger manual health check
     */
    @PostMapping("/monitoring/health-check")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger manual policy health check",
               description = "Manually trigger policy health check and alert evaluation")
    public ResponseEntity<ApiResponse<String>> triggerHealthCheck() {
        log.info("Manual policy health check triggered");
        monitoringService.checkPolicyHealth();
        return ResponseEntity.ok(ApiResponse.success("Health check completed successfully"));
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Casbin cache statistics",
               description = "Returns L1 cache hit rate, size, and performance metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats() {
        log.debug("Getting Casbin cache statistics");
        Map<String, Object> stats = cacheService.getCacheStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Clear all Casbin caches
     */
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Clear all Casbin caches",
               description = "Clears L1 cache and resets statistics. Use after policy changes.")
    public ResponseEntity<ApiResponse<String>> clearCaches() {
        log.warn("Clearing all Casbin caches");
        cacheService.clearAll();
        return ResponseEntity.ok(ApiResponse.success("All caches cleared successfully"));
    }

    /**
     * Clear cache for specific user
     */
    @PostMapping("/cache/clear/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Clear cache for specific user",
               description = "Clears cache entries for a specific user. Use after role changes.")
    public ResponseEntity<ApiResponse<String>> clearUserCache(@PathVariable String userId) {
        log.info("Clearing cache for user: {}", userId);
        cacheService.invalidateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Cache cleared for user: " + userId));
    }

    /**
     * Clear cache for specific tenant
     */
    @PostMapping("/cache/clear/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Clear cache for specific tenant",
               description = "Clears cache entries for a specific tenant. Use after policy changes.")
    public ResponseEntity<ApiResponse<String>> clearTenantCache(@PathVariable String tenantId) {
        log.info("Clearing cache for tenant: {}", tenantId);
        cacheService.invalidateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Cache cleared for tenant: " + tenantId));
    }

    /**
     * Clear cache for specific role
     */
    @PostMapping("/cache/clear/role/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Clear cache for specific role",
               description = "Clears cache entries for a specific role. Use after permission changes.")
    public ResponseEntity<ApiResponse<String>> clearRoleCache(@PathVariable String roleId) {
        log.info("Clearing cache for role: {}", roleId);
        cacheService.invalidateRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Cache cleared for role: " + roleId));
    }

    /**
     * Get role hierarchy structure
     */
    @GetMapping("/hierarchy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get role hierarchy structure",
               description = "Returns complete role hierarchy tree showing inheritance relationships")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHierarchy() {
        log.debug("Getting role hierarchy structure");
        Map<String, Object> hierarchy = hierarchyService.getHierarchyStructure();
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    /**
     * Add role inheritance
     */
    @PostMapping("/hierarchy/inherit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add role inheritance",
               description = "Make a role inherit permissions from a parent role")
    public ResponseEntity<ApiResponse<String>> addRoleInheritance(
            @RequestParam String role,
            @RequestParam String parentRole,
            @RequestParam String domain) {

        log.info("Adding role inheritance: {} inherits from {} (domain: {})",
                 role, parentRole, domain);

        boolean success = hierarchyService.addRoleInheritance(role, parentRole, domain);

        if (success) {
            // Clear cache after hierarchy change
            cacheService.invalidateRole(role);
            return ResponseEntity.ok(ApiResponse.success(
                    "Role inheritance added: " + role + " now inherits from " + parentRole));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Failed to add role inheritance (may already exist)"));
        }
    }

    /**
     * Remove role inheritance
     */
    @DeleteMapping("/hierarchy/inherit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role inheritance",
               description = "Remove inheritance relationship between roles")
    public ResponseEntity<ApiResponse<String>> removeRoleInheritance(
            @RequestParam String role,
            @RequestParam String parentRole,
            @RequestParam String domain) {

        log.info("Removing role inheritance: {} no longer inherits from {} (domain: {})",
                 role, parentRole, domain);

        boolean success = hierarchyService.removeRoleInheritance(role, parentRole, domain);

        if (success) {
            // Clear cache after hierarchy change
            cacheService.invalidateRole(role);
            return ResponseEntity.ok(ApiResponse.success(
                    "Role inheritance removed: " + role + " no longer inherits from " + parentRole));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Failed to remove role inheritance (may not exist)"));
        }
    }

    /**
     * Validate if policies can be added
     */
    @GetMapping("/monitoring/validate-add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Validate if policies can be added",
               description = "Check if adding new policies would exceed tenant or role limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateAddPolicies(
            @RequestParam String tenant,
            @RequestParam String role,
            @RequestParam int count) {

        log.debug("Validating add {} policies to role {} in tenant {}", count, role, tenant);

        boolean canAdd = monitoringService.canAddPolicies(tenant, role, count);

        Map<String, Object> result = Map.of(
                "can_add", canAdd,
                "tenant", tenant,
                "role", role,
                "policy_count", count,
                "message", canAdd ? "Policies can be added safely" :
                        "Adding policies would exceed limits"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
