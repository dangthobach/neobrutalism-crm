package com.neobrutalism.crm.domain.permission.service;

import com.neobrutalism.crm.common.security.PermissionService;
import com.neobrutalism.crm.domain.permission.dto.PermissionMatrixDTO;
import com.neobrutalism.crm.domain.permission.event.PermissionChangedEvent;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission Matrix Service
 * 
 * Provides matrix operations for viewing and managing permissions in a grid format.
 * The matrix shows roles as rows and resources as columns, with permissions as cells.
 * 
 * Features:
 * - Build permission matrix from Casbin policies
 * - Support role hierarchy inheritance
 * - Bulk permission updates
 * - Cache integration for performance
 * 
 * @author Neobrutalism CRM Team
 */
@Service
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class PermissionMatrixService {

    private final Enforcer enforcer;
    private final PermissionService permissionService;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Get permission matrix for a tenant
     * Cached: 1 hour TTL, key by tenant ID
     * 
     * @param tenantId Tenant/Organization ID
     * @return Permission matrix DTO
     */
    @Cacheable(value = "permissionMatrix", key = "#tenantId")
    public PermissionMatrixDTO getPermissionMatrix(String tenantId) {
        log.debug("Building permission matrix for tenant: {}", tenantId);

        // Get all roles for this tenant
        List<Role> roles = getRolesForTenant(tenantId);
        
        // Build resource list from policies
        Set<String> resourceSet = new HashSet<>();
        Set<String> actionSet = new HashSet<>();
        
        // Collect all resources and actions from policies
        List<List<String>> allPolicies = enforcer.getPolicy();
        for (List<String> policy : allPolicies) {
            if (policy.size() >= 4) {
                String policyTenant = policy.get(1);
                if (tenantId.equals(policyTenant)) {
                    resourceSet.add(policy.get(2)); // resource
                    String action = policy.get(3);
                    // Parse action pattern (e.g., "(GET)|(POST)" -> ["GET", "POST"])
                    parseActions(action, actionSet);
                }
            }
        }

        // Build role info list
        List<PermissionMatrixDTO.RoleInfo> roleInfos = roles.stream()
                .map(role -> PermissionMatrixDTO.RoleInfo.builder()
                        .roleCode(role.getCode())
                        .roleName(role.getName())
                        .description(role.getDescription())
                        .priority(role.getPriority())
                        .scope(getScopeForRole(role))
                        .inherited(false) // Will be calculated based on hierarchy
                        .build())
                .sorted(Comparator.comparing(PermissionMatrixDTO.RoleInfo::getPriority).reversed()
                        .thenComparing(PermissionMatrixDTO.RoleInfo::getRoleCode))
                .collect(Collectors.toList());

        // Build resource info list
        List<String> sortedResources = resourceSet.stream()
                .sorted()
                .collect(Collectors.toList());
        
        List<String> sortedActions = actionSet.stream()
                .sorted()
                .collect(Collectors.toList());

        List<PermissionMatrixDTO.ResourceInfo> resourceInfos = sortedResources.stream()
                .map(resource -> PermissionMatrixDTO.ResourceInfo.builder()
                        .path(resource)
                        .name(extractResourceName(resource))
                        .category(extractCategory(resource))
                        .availableActions(sortedActions)
                        .build())
                .collect(Collectors.toList());

        // Build permission matrix
        Map<String, Map<String, List<String>>> matrix = new HashMap<>();
        
        for (Role role : roles) {
            Map<String, List<String>> rolePermissions = new HashMap<>();
            
            // Get all policies for this role in this tenant
            List<List<String>> rolePolicies = enforcer.getFilteredPolicy(0, role.getCode(), tenantId);
            
            for (List<String> policy : rolePolicies) {
                if (policy.size() >= 4) {
                    String resource = policy.get(2);
                    String action = policy.get(3);
                    
                    rolePermissions.computeIfAbsent(resource, k -> new ArrayList<>())
                            .addAll(parseActionList(action));
                }
            }
            
            // Remove duplicates and sort
            rolePermissions.forEach((resource, actions) -> {
                List<String> uniqueActions = actions.stream()
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                rolePermissions.put(resource, uniqueActions);
            });
            
            matrix.put(role.getCode(), rolePermissions);
        }

        // Build role hierarchy map
        Map<String, List<String>> hierarchy = buildRoleHierarchy(tenantId);

        return PermissionMatrixDTO.builder()
                .tenantId(tenantId)
                .roles(roleInfos)
                .resources(resourceInfos)
                .matrix(matrix)
                .hierarchy(hierarchy)
                .build();
    }

    /**
     * Update permissions in bulk
     * Cache eviction: Clears permission matrix cache for the tenant
     * 
     * @param bulkUpdate Bulk update request
     * @return Number of updates applied
     */
    @Transactional
    @CacheEvict(value = "permissionMatrix", key = "#bulkUpdate.tenantId")
    public int updatePermissionsBulk(PermissionMatrixDTO.BulkPermissionUpdate bulkUpdate) {
        log.info("Bulk updating permissions for tenant: {}, updates: {}", 
                bulkUpdate.getTenantId(), bulkUpdate.getUpdates().size());

        int successCount = 0;
        String reason = bulkUpdate.getReason() != null 
                ? bulkUpdate.getReason() 
                : "Bulk permission update via Permission Matrix";

        for (PermissionMatrixDTO.PermissionUpdate update : bulkUpdate.getUpdates()) {
            try {
                String roleCode = update.getRoleCode();
                String resource = update.getResource();
                List<String> actions = update.getActions();

                // Get current permissions for this role/resource
                List<List<String>> currentPolicies = enforcer.getFilteredPolicy(
                        0, roleCode, bulkUpdate.getTenantId(), resource);

                // Remove all existing policies for this role/resource combination
                for (List<String> policy : currentPolicies) {
                    if (policy.size() >= 4) {
                        String currentAction = policy.get(3);
                        permissionService.removePermissionFromRole(
                                roleCode, bulkUpdate.getTenantId(), resource, currentAction, reason);
                    }
                }

                // Add new permissions
                if (actions != null && !actions.isEmpty()) {
                    for (String action : actions) {
                        boolean success = permissionService.addPermissionForRole(
                                roleCode, bulkUpdate.getTenantId(), resource, action, reason);
                        if (success) {
                            successCount++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update permission: role={}, resource={}", 
                        update.getRoleCode(), update.getResource(), e);
            }
        }

        // Publish bulk update event
        if (successCount > 0) {
            String currentUsername = getCurrentUsername();
            UUID currentUserId = getCurrentUserId();
            
            PermissionChangedEvent event = PermissionChangedEvent.bulkUpdate(
                    bulkUpdate.getTenantId(),
                    currentUserId,
                    currentUsername,
                    reason,
                    String.format("Updated %d permissions", successCount)
            );
            eventPublisher.publishEvent(event);
            log.debug("Published PermissionChangedEvent: BULK_UPDATE");
        }

        log.info("Bulk update completed: {} permissions updated", successCount);
        return successCount;
    }

    /**
     * Update a single permission cell
     * Cache eviction: Clears permission matrix cache for the tenant
     * 
     * @param tenantId Tenant ID
     * @param update Permission update request
     * @return true if successful
     */
    @Transactional
    @CacheEvict(value = "permissionMatrix", key = "#tenantId")
    public boolean updatePermission(String tenantId, PermissionMatrixDTO.PermissionUpdate update) {
        log.debug("Updating permission: tenant={}, role={}, resource={}", 
                tenantId, update.getRoleCode(), update.getResource());

        String roleCode = update.getRoleCode();
        String resource = update.getResource();
        List<String> actions = update.getActions();
        String reason = update.getReason() != null 
                ? update.getReason() 
                : "Permission update via Permission Matrix";

        // Remove existing policies for this role/resource
        List<List<String>> currentPolicies = enforcer.getFilteredPolicy(
                0, roleCode, tenantId, resource);
        
        for (List<String> policy : currentPolicies) {
            if (policy.size() >= 4) {
                String currentAction = policy.get(3);
                permissionService.removePermissionFromRole(
                        roleCode, tenantId, resource, currentAction, reason);
            }
        }

        // Add new permissions
        boolean allSuccess = true;
        if (actions != null && !actions.isEmpty()) {
            for (String action : actions) {
                boolean success = permissionService.addPermissionForRole(
                        roleCode, tenantId, resource, action, reason);
                allSuccess = allSuccess && success;
            }
        }

        return allSuccess;
    }

    /**
     * Get roles for tenant (system roles + organization roles)
     */
    private List<Role> getRolesForTenant(String tenantId) {
        List<Role> roles = new ArrayList<>();
        
        // Add system roles
        roles.addAll(roleRepository.findByIsSystemTrue());
        
        // Add organization roles if tenantId is a UUID
        try {
            UUID orgId = UUID.fromString(tenantId);
            roles.addAll(roleRepository.findByOrganizationId(orgId));
        } catch (IllegalArgumentException e) {
            // tenantId is not a UUID, skip organization roles
            log.debug("Tenant ID is not a UUID, skipping organization roles: {}", tenantId);
        }
        
        return roles;
    }

    /**
     * Build role hierarchy map
     */
    private Map<String, List<String>> buildRoleHierarchy(String tenantId) {
        Map<String, List<String>> hierarchy = new HashMap<>();
        
        // Get all role hierarchy policies (g2)
        List<List<String>> hierarchyPolicies = enforcer.getFilteredGroupingPolicy(1, tenantId);
        
        for (List<String> policy : hierarchyPolicies) {
            if (policy.size() >= 3) {
                String childRole = policy.get(0);
                String parentRole = policy.get(1);
                
                hierarchy.computeIfAbsent(childRole, k -> new ArrayList<>())
                        .add(parentRole);
            }
        }
        
        return hierarchy;
    }

    /**
     * Parse action pattern into individual actions
     * Handles patterns like "(GET)|(POST)" -> ["GET", "POST"]
     */
    private void parseActions(String actionPattern, Set<String> actionSet) {
        if (actionPattern == null || actionPattern.isEmpty()) {
            return;
        }

        // Handle wildcard
        if ("*".equals(actionPattern)) {
            actionSet.add("*");
            return;
        }

        // Handle pattern like "(GET)|(POST)|(PUT)"
        String[] parts = actionPattern.split("\\|");
        for (String part : parts) {
            String cleaned = part.trim()
                    .replaceAll("^\\(", "")
                    .replaceAll("\\)$", "");
            if (!cleaned.isEmpty()) {
                actionSet.add(cleaned);
            }
        }
    }

    /**
     * Parse action pattern into list of actions
     */
    private List<String> parseActionList(String actionPattern) {
        Set<String> actionSet = new HashSet<>();
        parseActions(actionPattern, actionSet);
        return new ArrayList<>(actionSet);
    }

    /**
     * Extract resource name from path
     * Example: "/api/users" -> "Users"
     */
    private String extractResourceName(String resource) {
        if (resource == null || resource.isEmpty()) {
            return "Unknown";
        }

        // Remove leading/trailing slashes and wildcards
        String cleaned = resource.replaceAll("^/api/", "")
                .replaceAll("/$", "")
                .replaceAll("\\*$", "");

        // Convert to title case
        if (cleaned.isEmpty()) {
            return "Root";
        }

        String[] parts = cleaned.split("/");
        String lastPart = parts[parts.length - 1];
        
        // Capitalize first letter
        return lastPart.substring(0, 1).toUpperCase() + 
               lastPart.substring(1).toLowerCase();
    }

    /**
     * Extract category from resource path
     * Example: "/api/users" -> "Users"
     */
    private String extractCategory(String resource) {
        if (resource == null || resource.isEmpty()) {
            return "Other";
        }

        if (resource.startsWith("/api/users")) {
            return "Users";
        } else if (resource.startsWith("/api/tasks")) {
            return "Tasks";
        } else if (resource.startsWith("/api/contacts")) {
            return "Contacts";
        } else if (resource.startsWith("/api/customers")) {
            return "Customers";
        } else if (resource.startsWith("/api/organizations")) {
            return "Organizations";
        } else if (resource.startsWith("/api/roles")) {
            return "Roles";
        } else {
            return "Other";
        }
    }

    /**
     * Get scope for role (from role's data scope if available)
     */
    private String getScopeForRole(Role role) {
        // TODO: Implement if role has data scope field
        return "ALL_BRANCHES"; // Default
    }

    /**
     * Get current authenticated user ID
     */
    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.neobrutalism.crm.domain.user.model.User) {
                return ((com.neobrutalism.crm.domain.user.model.User) authentication.getPrincipal()).getId();
            }
        } catch (Exception e) {
            log.warn("Failed to get current user ID from security context", e);
        }
        return null;
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Failed to get current username from security context", e);
        }
        return "system";
    }
}

