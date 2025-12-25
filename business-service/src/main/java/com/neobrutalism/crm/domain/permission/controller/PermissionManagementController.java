package com.neobrutalism.crm.domain.permission.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.config.security.CasbinPolicyManager;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Casbin permission management
 * Provides endpoints for reloading and syncing Casbin policies
 */
@RestController
@RequestMapping("/api/permissions/casbin")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "Casbin policy management APIs")
public class PermissionManagementController {

    private final CasbinPolicyManager casbinPolicyManager;
    private final RoleService roleService;
    private final com.neobrutalism.crm.config.security.CasbinCacheService casbinCacheService;

    @PostMapping("/reload")
    @Operation(
        summary = "Reload all Casbin policies from database",
        description = "Clears and reloads all Casbin policies from role_menus table. Use this after bulk updates or manual database changes."
    )
    public ApiResponse<Map<String, Object>> reloadAllPolicies() {
        long startTime = System.currentTimeMillis();

        casbinPolicyManager.reloadAllPolicies();

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reloadedAt", Instant.now().toString());
        result.put("durationMs", duration);

        return ApiResponse.success("All Casbin policies reloaded successfully", result);
    }

    @PostMapping("/sync-role/{roleId}")
    @Operation(
        summary = "Sync Casbin policies for a specific role",
        description = "Synchronizes Casbin policies for a single role from role_menus table. Useful for testing or targeted updates."
    )
    public ApiResponse<Map<String, Object>> syncRolePolicies(@PathVariable UUID roleId) {
        Role role = roleService.findById(roleId);

        long startTime = System.currentTimeMillis();
        int policyCount = casbinPolicyManager.syncRolePolicies(role, true);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("roleId", roleId.toString());
        result.put("roleName", role.getName());
        result.put("roleCode", role.getCode());
        result.put("policiesSynced", policyCount);
        result.put("syncedAt", Instant.now().toString());
        result.put("durationMs", duration);

        return ApiResponse.success(
            String.format("Synced %d policies for role: %s", policyCount, role.getName()),
            result
        );
    }

    @GetMapping("/policies")
    @Operation(
        summary = "Get all Casbin policies",
        description = "Retrieves all currently loaded Casbin policies for inspection and debugging"
    )
    public ApiResponse<Map<String, Object>> getAllPolicies() {
        List<List<String>> policies = casbinPolicyManager.getAllPolicies();
        List<List<String>> groupingPolicies = casbinPolicyManager.getAllGroupingPolicies();

        Map<String, Object> result = new HashMap<>();
        result.put("policies", policies);
        result.put("policyCount", policies.size());
        result.put("groupingPolicies", groupingPolicies);
        result.put("groupingPolicyCount", groupingPolicies.size());
        result.put("totalCount", policies.size() + groupingPolicies.size());

        return ApiResponse.success("Retrieved all Casbin policies", result);
    }

    @GetMapping("/check")
    @Operation(
        summary = "Check permission for user",
        description = "Test if a user/role has permission to access a specific resource with an action"
    )
    public ApiResponse<Map<String, Object>> checkPermission(
            @RequestParam String user,
            @RequestParam String domain,
            @RequestParam String resource,
            @RequestParam String action) {

        boolean hasPermission = casbinPolicyManager.checkPermission(user, domain, resource, action);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("domain", domain);
        result.put("resource", resource);
        result.put("action", action);
        result.put("hasPermission", hasPermission);
        result.put("checkedAt", Instant.now().toString());

        return ApiResponse.success(
            hasPermission ? "Permission granted" : "Permission denied",
            result
        );
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Get Casbin policy statistics",
        description = "Returns statistics about loaded policies, useful for monitoring"
    )
    public ApiResponse<Map<String, Object>> getPolicyStats() {
        List<List<String>> policies = casbinPolicyManager.getAllPolicies();
        List<List<String>> groupingPolicies = casbinPolicyManager.getAllGroupingPolicies();

        // Count policies by type
        long allowPolicies = policies.stream()
            .filter(p -> p.size() >= 5 && "allow".equals(p.get(4)))
            .count();

        long denyPolicies = policies.stream()
            .filter(p -> p.size() >= 5 && "deny".equals(p.get(4)))
            .count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalPolicies", policies.size());
        result.put("allowPolicies", allowPolicies);
        result.put("denyPolicies", denyPolicies);
        result.put("groupingPolicies", groupingPolicies.size());
        result.put("timestamp", Instant.now().toString());

        return ApiResponse.success("Casbin policy statistics", result);
    }

    @GetMapping("/cache/stats")
    @Operation(
        summary = "Get L1 cache statistics",
        description = "Returns L1 (Caffeine) cache statistics including hit rate, size, and performance metrics"
    )
    public ApiResponse<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = casbinCacheService.getCacheStats();
        return ApiResponse.success("L1 cache statistics", stats);
    }

    @PostMapping("/cache/clear")
    @Operation(
        summary = "Clear L1 cache",
        description = "Manually clears all L1 cache entries. Use this after manual database changes or for troubleshooting."
    )
    public ApiResponse<Map<String, Object>> clearCache() {
        casbinCacheService.clearAll();

        Map<String, Object> result = new HashMap<>();
        result.put("cleared", true);
        result.put("clearedAt", Instant.now().toString());

        return ApiResponse.success("L1 cache cleared successfully", result);
    }

    @PostMapping("/cache/invalidate/role/{roleName}")
    @Operation(
        summary = "Invalidate L1 cache for a specific role",
        description = "Invalidates L1 cache entries for users with the specified role. Use after role permission changes."
    )
    public ApiResponse<Map<String, Object>> invalidateRoleCache(@PathVariable String roleName) {
        casbinCacheService.invalidateRole(roleName);

        Map<String, Object> result = new HashMap<>();
        result.put("role", roleName);
        result.put("invalidatedAt", Instant.now().toString());

        return ApiResponse.success("L1 cache invalidated for role: " + roleName, result);
    }

    @PostMapping("/cache/invalidate/tenant/{tenantId}")
    @Operation(
        summary = "Invalidate L1 cache for a specific tenant",
        description = "Invalidates L1 cache entries for the specified tenant. Use after tenant-wide policy changes."
    )
    public ApiResponse<Map<String, Object>> invalidateTenantCache(@PathVariable String tenantId) {
        casbinCacheService.invalidateTenant(tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("tenant", tenantId);
        result.put("invalidatedAt", Instant.now().toString());

        return ApiResponse.success("L1 cache invalidated for tenant: " + tenantId, result);
    }
}
