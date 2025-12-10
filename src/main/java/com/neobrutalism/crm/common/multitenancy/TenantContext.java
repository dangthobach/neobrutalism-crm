package com.neobrutalism.crm.common.multitenancy;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe tenant context holder
 * Stores the current tenant ID for the current request thread
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "default";

    /**
     * Set the current tenant ID for this thread
     */
    public static void setCurrentTenant(String tenantId) {
        log.debug("Setting tenant context: {}", tenantId);
        currentTenant.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread
     * Returns null if not set (for explicit null checks)
     */
    public static String getCurrentTenant() {
        String tenantId = currentTenant.get();
        if (tenantId == null) {
            log.debug("No tenant context set for current thread (background thread or not yet set)");
        }
        return tenantId;
    }

    /**
     * Get the current tenant ID for this thread, or default tenant if not set
     * Use this method when you need a tenant ID and can accept default tenant for background threads
     */
    public static String getCurrentTenantOrDefault() {
        String tenantId = currentTenant.get();
        return tenantId != null ? tenantId : DEFAULT_TENANT;
    }

    /**
     * Clear the tenant context for this thread
     */
    public static void clear() {
        log.debug("Clearing tenant context: {}", currentTenant.get());
        currentTenant.remove();
    }

    /**
     * Check if tenant context is set
     */
    public static boolean isSet() {
        return currentTenant.get() != null;
    }
}
