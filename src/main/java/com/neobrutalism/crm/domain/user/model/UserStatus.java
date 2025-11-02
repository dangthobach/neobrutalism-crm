package com.neobrutalism.crm.domain.user.model;

/**
 * User status enumeration
 */
public enum UserStatus {
    /**
     * Pending activation
     */
    PENDING,

    /**
     * Active and operational
     */
    ACTIVE,

    /**
     * Temporarily suspended
     */
    SUSPENDED,

    /**
     * Locked (due to failed login attempts)
     */
    LOCKED,

    /**
     * Inactive/Disabled
     */
    INACTIVE
}
