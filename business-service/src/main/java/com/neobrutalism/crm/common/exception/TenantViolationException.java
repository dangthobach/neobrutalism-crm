package com.neobrutalism.crm.common.exception;

/**
 * ✅ PHASE 1: Exception thrown when tenant isolation is violated
 * Occurs when user tries to access resources from another organization
 */
public class TenantViolationException extends BaseException {

    public TenantViolationException(String message) {
        super(message, "TENANT_VIOLATION");
    }

    public TenantViolationException(String message, String tenantId, String attemptedTenantId) {
        super(
            String.format("%s. Your tenant: %s, Attempted access to: %s", 
                message, tenantId, attemptedTenantId
            ),
            "TENANT_VIOLATION"
        );
    }
}
