package com.neobrutalism.crm.common.security;

/**
 * Tenant Context - Thread-local storage for tenant ID
 */
public class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    private static String DEFAULT_TENANT_ID = "default";

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = TENANT_ID.get();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    public static void setDefaultTenantId(String defaultId) {
        DEFAULT_TENANT_ID = defaultId;
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    public static boolean hasTenantId() {
        return TENANT_ID.get() != null;
    }
}
