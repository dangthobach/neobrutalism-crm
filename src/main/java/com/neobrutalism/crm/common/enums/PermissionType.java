package com.neobrutalism.crm.common.enums;

/**
 * Permission types for API access control
 */
public enum PermissionType {
    /**
     * View/Read permission
     */
    READ,

    /**
     * Create/Update permission
     */
    WRITE,

    /**
     * Delete permission
     */
    DELETE,

    /**
     * Execute actions (approve, publish, etc.)
     */
    EXECUTE
}
