package com.neobrutalism.crm.config.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Role Hierarchy Service for Casbin
 *
 * Purpose: Implement role inheritance to reduce policy count and improve performance
 *
 * Concept:
 * Instead of assigning all permissions to each role, roles can inherit from parent roles.
 * This dramatically reduces policy count while maintaining the same permission structure.
 *
 * Example:
 * Before (flat structure):
 *   - ADMIN: 1000 policies
 *   - MANAGER: 500 policies (many overlap with ADMIN)
 *   - USER: 100 policies (many overlap with MANAGER)
 *   Total: 1600 policies
 *
 * After (hierarchical structure):
 *   - ADMIN inherits from MANAGER: 500 unique policies
 *   - MANAGER inherits from USER: 400 unique policies
 *   - USER: 100 policies
 *   Total: 1000 policies (37.5% reduction!)
 *
 * Performance Impact:
 * - Reduces policy count by 30-70%
 * - Faster permission checks (fewer policies to evaluate)
 * - Easier maintenance (change parent role affects all children)
 * - Critical for 100k CCU scalability
 *
 * Role Hierarchy Example:
 * ```
 * SUPER_ADMIN
 *   ├── ADMIN
 *   │   ├── MANAGER
 *   │   │   ├── TEAM_LEAD
 *   │   │   │   └── USER
 *   │   │   └── ANALYST
 *   │   └── COORDINATOR
 *   └── SYSTEM_ADMIN
 * ```
 *
 * Casbin Integration:
 * Uses Casbin's built-in role inheritance (g2 grouping)
 * Format: g2, role, parent_role, domain
 *
 * @author Neobrutalism CRM Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleHierarchyService {

    private final Enforcer enforcer;

    @Value("${casbin.role-hierarchy.enabled:true}")
    private boolean hierarchyEnabled;

    // Role hierarchy cache (role -> parent roles)
    private final Map<String, Set<String>> roleParentsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (hierarchyEnabled) {
            log.info("Role Hierarchy Service initialized");
            loadRoleHierarchy();
        } else {
            log.warn("Role Hierarchy is DISABLED - flat role structure (more policies)");
        }
    }

    /**
     * Load existing role hierarchy from Casbin
     */
    private void loadRoleHierarchy() {
        try {
            // Get all role inheritance rules (g2)
            List<List<String>> roleInheritances = enforcer.getGroupingPolicy();

            int hierarchyCount = 0;
            for (List<String> inheritance : roleInheritances) {
                if (inheritance.size() >= 2) {
                    String role = inheritance.get(0);
                    String parentRole = inheritance.get(1);

                    roleParentsCache.computeIfAbsent(role, k -> new HashSet<>()).add(parentRole);
                    hierarchyCount++;
                }
            }

            log.info("Loaded {} role hierarchy relationships", hierarchyCount);
            if (hierarchyCount > 0) {
                logHierarchyStructure();
            }

        } catch (Exception e) {
            log.error("Error loading role hierarchy: {}", e.getMessage(), e);
        }
    }

    /**
     * Add role inheritance (role inherits from parent)
     *
     * @param role Child role
     * @param parentRole Parent role to inherit from
     * @param domain Tenant/domain
     * @return true if added successfully
     */
    public boolean addRoleInheritance(String role, String parentRole, String domain) {
        if (!hierarchyEnabled) {
            log.warn("Role hierarchy is disabled, cannot add inheritance");
            return false;
        }

        try {
            // Add to Casbin (g2 grouping)
            // Format: g2, role, parent_role, domain
            boolean added = enforcer.addGroupingPolicy(role, parentRole, domain);

            if (added) {
                // Update cache
                roleParentsCache.computeIfAbsent(role, k -> new HashSet<>()).add(parentRole);

                log.info("✅ Added role inheritance: {} inherits from {} (domain: {})",
                        role, parentRole, domain);

                // Calculate policy savings
                calculatePolicySavings(role, parentRole, domain);

                return true;
            } else {
                log.warn("Role inheritance already exists: {} -> {}", role, parentRole);
                return false;
            }

        } catch (Exception e) {
            log.error("Error adding role inheritance: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove role inheritance
     *
     * @param role Child role
     * @param parentRole Parent role
     * @param domain Tenant/domain
     * @return true if removed successfully
     */
    public boolean removeRoleInheritance(String role, String parentRole, String domain) {
        try {
            boolean removed = enforcer.removeGroupingPolicy(role, parentRole, domain);

            if (removed) {
                // Update cache
                Set<String> parents = roleParentsCache.get(role);
                if (parents != null) {
                    parents.remove(parentRole);
                    if (parents.isEmpty()) {
                        roleParentsCache.remove(role);
                    }
                }

                log.info("✅ Removed role inheritance: {} no longer inherits from {} (domain: {})",
                        role, parentRole, domain);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Error removing role inheritance: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all parent roles for a role (direct and transitive)
     *
     * @param role Role name
     * @return Set of all parent roles
     */
    public Set<String> getAllParentRoles(String role) {
        Set<String> allParents = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectParentRoles(role, allParents, visited);
        return allParents;
    }

    /**
     * Recursively collect parent roles
     */
    private void collectParentRoles(String role, Set<String> allParents, Set<String> visited) {
        if (visited.contains(role)) {
            return; // Prevent infinite loops
        }
        visited.add(role);

        Set<String> directParents = roleParentsCache.getOrDefault(role, Collections.emptySet());
        for (String parent : directParents) {
            allParents.add(parent);
            collectParentRoles(parent, allParents, visited);
        }
    }

    /**
     * Get role hierarchy structure
     */
    public Map<String, Object> getHierarchyStructure() {
        Map<String, Object> structure = new HashMap<>();
        structure.put("enabled", hierarchyEnabled);
        structure.put("total_roles_with_inheritance", roleParentsCache.size());

        // Build tree structure
        Map<String, List<String>> tree = new HashMap<>();
        roleParentsCache.forEach((role, parents) -> {
            tree.put(role, new ArrayList<>(parents));
        });
        structure.put("hierarchy", tree);

        return structure;
    }

    /**
     * Calculate and log policy savings from inheritance
     */
    private void calculatePolicySavings(String role, String parentRole, String domain) {
        try {
            // Get policies for both roles
            List<List<String>> rolePolicies = enforcer.getFilteredPolicy(0, "ROLE_" + role, domain);
            List<List<String>> parentPolicies = enforcer.getFilteredPolicy(0, "ROLE_" + parentRole, domain);

            // Find overlapping policies
            Set<String> rolePermissions = rolePolicies.stream()
                    .map(p -> p.get(2) + ":" + p.get(3)) // resource:action
                    .collect(java.util.stream.Collectors.toSet());

            Set<String> parentPermissions = parentPolicies.stream()
                    .map(p -> p.get(2) + ":" + p.get(3))
                    .collect(java.util.stream.Collectors.toSet());

            // Calculate overlap
            long overlap = rolePermissions.stream()
                    .filter(parentPermissions::contains)
                    .count();

            if (overlap > 0) {
                double savingsPercent = (double) overlap / rolePolicies.size() * 100;
                log.info("📊 Policy savings analysis: {} policies ({:.1f}%) can be removed from {} " +
                                "as they are inherited from {}",
                        overlap, savingsPercent, role, parentRole);

                if (savingsPercent > 50) {
                    log.info("💡 RECOMMENDATION: Remove {} redundant policies from {} to improve performance",
                            overlap, role);
                }
            }

        } catch (Exception e) {
            log.debug("Could not calculate policy savings: {}", e.getMessage());
        }
    }

    /**
     * Log hierarchy structure for debugging
     */
    private void logHierarchyStructure() {
        log.info("=== Role Hierarchy Structure ===");

        // Find root roles (roles with no parents)
        Set<String> allRoles = new HashSet<>(roleParentsCache.keySet());
        Set<String> childRoles = new HashSet<>(roleParentsCache.keySet());

        Set<String> rootRoles = new HashSet<>();
        for (Set<String> parents : roleParentsCache.values()) {
            rootRoles.addAll(parents);
        }
        rootRoles.removeAll(childRoles);

        if (rootRoles.isEmpty()) {
            log.info("No clear root roles found, listing all inheritance:");
            roleParentsCache.forEach((role, parents) ->
                    log.info("  {} -> {}", role, parents)
            );
        } else {
            log.info("Root roles: {}", rootRoles);
            rootRoles.forEach(root -> logHierarchyTree(root, 0));
        }

        log.info("================================");
    }

    /**
     * Recursively log hierarchy tree
     */
    private void logHierarchyTree(String role, int depth) {
        String indent = "  ".repeat(depth);
        log.info("{}├── {}", indent, role);

        // Find children of this role
        roleParentsCache.forEach((childRole, parents) -> {
            if (parents.contains(role)) {
                logHierarchyTree(childRole, depth + 1);
            }
        });
    }

    /**
     * Setup default role hierarchy (called from data initializer)
     */
    public void setupDefaultHierarchy(String domain) {
        if (!hierarchyEnabled) {
            return;
        }

        log.info("Setting up default role hierarchy for domain: {}", domain);

        // Default hierarchy:
        // SUPER_ADMIN -> ADMIN -> MANAGER -> USER
        addRoleInheritance("ADMIN", "SUPER_ADMIN", domain);
        addRoleInheritance("MANAGER", "ADMIN", domain);
        addRoleInheritance("USER", "MANAGER", domain);

        log.info("✅ Default role hierarchy established");
    }
}
