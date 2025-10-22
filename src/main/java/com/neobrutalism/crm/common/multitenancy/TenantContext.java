package com.neobrutalism.crm.common.multitenancy;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe tenant context holder
 * Stores the current tenant ID for the current request thread
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this thread
     */
    public static void setCurrentTenant(String tenantId) {
        log.debug("Setting tenant context: {}", tenantId);
        currentTenant.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread
     */
    public static String getCurrentTenant() {
        String tenantId = currentTenant.get();
        if (tenantId == null) {
            log.warn("No tenant context set for current thread");
        }
        return tenantId;
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
