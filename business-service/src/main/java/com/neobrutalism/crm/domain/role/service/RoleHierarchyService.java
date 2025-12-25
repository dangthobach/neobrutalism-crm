package com.neobrutalism.crm.domain.role.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleHierarchy;
import com.neobrutalism.crm.domain.role.repository.RoleHierarchyRepository;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing role hierarchy and inheritance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleHierarchyService {

    private final RoleHierarchyRepository hierarchyRepository;
    private final RoleRepository roleRepository;

    /**
     * Create a hierarchy relationship between parent and child roles
     */
    @Transactional
    public RoleHierarchy createHierarchy(
        UUID parentRoleId,
        UUID childRoleId,
        String tenantId,
        RoleHierarchy.InheritanceType inheritanceType
    ) {
        log.info("Creating role hierarchy: parent={}, child={}, tenant={}", parentRoleId, childRoleId, tenantId);

        // Validate roles exist
        Role parentRole = roleRepository.findById(parentRoleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Parent role not found"));

        Role childRole = roleRepository.findById(childRoleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Child role not found"));

        // Check if hierarchy already exists
        if (hierarchyRepository.existsByParentAndChild(parentRoleId, childRoleId, tenantId)) {
            throw new BusinessException(ErrorCode.ROLE_HIERARCHY_ALREADY_EXISTS,
                "Hierarchy relationship already exists between these roles");
        }

        // Check for circular dependency (would create infinite loop)
        if (wouldCreateCircularDependency(parentRoleId, childRoleId, tenantId)) {
            throw new BusinessException(ErrorCode.ROLE_HIERARCHY_CIRCULAR_DEPENDENCY,
                "Cannot create hierarchy: would create circular dependency");
        }

        // Calculate hierarchy level
        int hierarchyLevel = calculateHierarchyLevel(parentRoleId, tenantId);

        // Validate max depth
        if (hierarchyLevel > 10) {
            throw new BusinessException(ErrorCode.ROLE_HIERARCHY_MAX_DEPTH_EXCEEDED,
                "Cannot create hierarchy: maximum depth of 10 levels exceeded");
        }

        // Create hierarchy
        RoleHierarchy hierarchy = RoleHierarchy.builder()
            .parentRole(parentRole)
            .childRole(childRole)
            .hierarchyLevel(hierarchyLevel)
            .inheritanceType(inheritanceType)
            .active(true)
            .tenantId(tenantId)
            .build();

        return hierarchyRepository.save(hierarchy);
    }

    /**
     * Remove a hierarchy relationship
     */
    @Transactional
    public void removeHierarchy(UUID parentRoleId, UUID childRoleId, String tenantId) {
        log.info("Removing role hierarchy: parent={}, child={}, tenant={}", parentRoleId, childRoleId, tenantId);

        RoleHierarchy hierarchy = hierarchyRepository.findByParentAndChild(parentRoleId, childRoleId, tenantId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_HIERARCHY_NOT_FOUND,
                "Hierarchy relationship not found"));

        hierarchy.setDeleted(true);
        hierarchyRepository.save(hierarchy);
    }

    /**
     * Get all parent roles (ancestors) for a role
     */
    @Transactional(readOnly = true)
    public List<Role> getAncestors(UUID roleId, String tenantId) {
        return hierarchyRepository.findAllAncestors(roleId, tenantId);
    }

    /**
     * Get all child roles (descendants) for a role
     */
    @Transactional(readOnly = true)
    public List<Role> getDescendants(UUID roleId, String tenantId) {
        return hierarchyRepository.findAllDescendants(roleId, tenantId);
    }

    /**
     * Get direct parents for a role
     */
    @Transactional(readOnly = true)
    public List<RoleHierarchy> getDirectParents(UUID roleId, String tenantId) {
        return hierarchyRepository.findParentsByChildRole(roleId, tenantId);
    }

    /**
     * Get direct children for a role
     */
    @Transactional(readOnly = true)
    public List<RoleHierarchy> getDirectChildren(UUID roleId, String tenantId) {
        return hierarchyRepository.findChildrenByParentRole(roleId, tenantId);
    }

    /**
     * Get all inherited role codes for a user's roles
     * Includes the user's roles plus all ancestor roles
     */
    @Transactional(readOnly = true)
    public Set<String> getInheritedRoleCodes(List<String> userRoleCodes, String tenantId) {
        Set<String> inheritedRoles = new HashSet<>(userRoleCodes);

        // For each user role, get all ancestors
        for (String roleCode : userRoleCodes) {
            Optional<Role> roleOpt = roleRepository.findByCodeAndDeletedFalse(roleCode);
            if (roleOpt.isPresent()) {
                List<Role> ancestors = getAncestors(roleOpt.get().getId(), tenantId);
                inheritedRoles.addAll(ancestors.stream()
                    .map(Role::getCode)
                    .collect(Collectors.toSet()));
            }
        }

        return inheritedRoles;
    }

    /**
     * Get the complete hierarchy tree for a tenant
     */
    @Transactional(readOnly = true)
    public List<RoleHierarchy> getHierarchyTree(String tenantId) {
        return hierarchyRepository.findActiveByTenant(tenantId);
    }

    /**
     * Check if a role is an ancestor of another role
     */
    @Transactional(readOnly = true)
    public boolean isAncestor(UUID ancestorRoleId, UUID descendantRoleId, String tenantId) {
        List<Role> ancestors = getAncestors(descendantRoleId, tenantId);
        return ancestors.stream()
            .anyMatch(role -> role.getId().equals(ancestorRoleId));
    }

    /**
     * Check if a role is a descendant of another role
     */
    @Transactional(readOnly = true)
    public boolean isDescendant(UUID descendantRoleId, UUID ancestorRoleId, String tenantId) {
        List<Role> descendants = getDescendants(ancestorRoleId, tenantId);
        return descendants.stream()
            .anyMatch(role -> role.getId().equals(descendantRoleId));
    }

    /**
     * Get hierarchy depth for a role
     */
    @Transactional(readOnly = true)
    public int getHierarchyDepth(UUID roleId, String tenantId) {
        return hierarchyRepository.getMaxHierarchyDepth(roleId, tenantId);
    }

    /**
     * Activate a hierarchy relationship
     */
    @Transactional
    public void activateHierarchy(UUID parentRoleId, UUID childRoleId, String tenantId) {
        RoleHierarchy hierarchy = hierarchyRepository.findByParentAndChild(parentRoleId, childRoleId, tenantId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_HIERARCHY_NOT_FOUND,
                "Hierarchy relationship not found"));

        hierarchy.setActive(true);
        hierarchyRepository.save(hierarchy);
    }

    /**
     * Deactivate a hierarchy relationship
     */
    @Transactional
    public void deactivateHierarchy(UUID parentRoleId, UUID childRoleId, String tenantId) {
        RoleHierarchy hierarchy = hierarchyRepository.findByParentAndChild(parentRoleId, childRoleId, tenantId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_HIERARCHY_NOT_FOUND,
                "Hierarchy relationship not found"));

        hierarchy.setActive(false);
        hierarchyRepository.save(hierarchy);
    }

    /**
     * Check if creating a hierarchy would create a circular dependency
     */
    private boolean wouldCreateCircularDependency(UUID parentRoleId, UUID childRoleId, String tenantId) {
        // Check if parent is already a descendant of child
        List<Role> descendants = hierarchyRepository.findAllDescendants(childRoleId, tenantId);
        return descendants.stream()
            .anyMatch(role -> role.getId().equals(parentRoleId));
    }

    /**
     * Calculate hierarchy level for a new relationship
     */
    private int calculateHierarchyLevel(UUID parentRoleId, String tenantId) {
        // Get the max depth of the parent role
        int parentDepth = hierarchyRepository.getMaxHierarchyDepth(parentRoleId, tenantId);
        // New hierarchy level is parent's depth + 1
        return parentDepth + 1;
    }

    /**
     * Build a visual hierarchy tree (for debugging/display)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> buildHierarchyTree(String tenantId) {
        List<RoleHierarchy> hierarchies = hierarchyRepository.findActiveByTenant(tenantId);

        // Group by parent role
        Map<UUID, List<RoleHierarchy>> byParent = hierarchies.stream()
            .collect(Collectors.groupingBy(h -> h.getParentRole().getId()));

        // Find root roles (roles with no parents)
        Set<UUID> allParents = hierarchies.stream()
            .map(h -> h.getParentRole().getId())
            .collect(Collectors.toSet());

        Set<UUID> allChildren = hierarchies.stream()
            .map(h -> h.getChildRole().getId())
            .collect(Collectors.toSet());

        Set<UUID> roots = new HashSet<>(allParents);
        roots.removeAll(allChildren);

        Map<String, Object> tree = new HashMap<>();
        tree.put("roots", roots);
        tree.put("hierarchies", byParent);

        return tree;
    }
}
