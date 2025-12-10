package com.neobrutalism.crm.domain.permission.model;

/**
 * Types of permission actions that can be audited
 */
public enum PermissionActionType {

    // Role Management
    ROLE_ASSIGNED("Role assigned to user"),
    ROLE_REMOVED("Role removed from user"),
    ROLE_CREATED("New role created"),
    ROLE_UPDATED("Role configuration updated"),
    ROLE_DELETED("Role deleted"),

    // Policy Management
    POLICY_CREATED("Permission policy created"),
    POLICY_UPDATED("Permission policy updated"),
    POLICY_DELETED("Permission policy deleted"),

    // Data Scope Changes
    DATA_SCOPE_CHANGED("User data scope changed"),
    BRANCH_ACCESS_GRANTED("Branch access granted"),
    BRANCH_ACCESS_REVOKED("Branch access revoked"),

    // Group/Team Management
    GROUP_ASSIGNED("User assigned to group"),
    GROUP_REMOVED("User removed from group"),

    // Permission Checks
    PERMISSION_DENIED("Permission check failed - access denied"),
    PERMISSION_GRANTED("Permission check succeeded"),

    // Bulk Operations
    BULK_ROLE_ASSIGNMENT("Bulk role assignment operation"),
    BULK_PERMISSION_UPDATE("Bulk permission update operation"),

    // System Events
    PERMISSION_CACHE_CLEARED("Permission cache cleared"),
    POLICY_RELOAD("Casbin policies reloaded"),

    // Security Events
    UNAUTHORIZED_ACCESS_ATTEMPT("Unauthorized access attempt"),
    PERMISSION_ESCALATION_ATTEMPT("Permission escalation attempt detected");

    private final String description;

    PermissionActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this action type is a security-critical event
     */
    public boolean isCritical() {
        return this == UNAUTHORIZED_ACCESS_ATTEMPT ||
               this == PERMISSION_ESCALATION_ATTEMPT ||
               this == DATA_SCOPE_CHANGED ||
               this == ROLE_ASSIGNED ||
               this == ROLE_REMOVED;
    }

    /**
     * Check if this action type requires admin approval
     */
    public boolean requiresApproval() {
        return this == ROLE_CREATED ||
               this == ROLE_DELETED ||
               this == BULK_ROLE_ASSIGNMENT ||
               this == BULK_PERMISSION_UPDATE;
    }
}
