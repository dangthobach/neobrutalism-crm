package com.neobrutalism.crm.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user lacks required permissions
 * Returns HTTP 403 Forbidden
 *
 * This exception is thrown by the PermissionCheckAspect when:
 * - User doesn't have the required permission for a resource
 * - @RequirePermission annotation check fails
 * - Tenant isolation check fails
 */
@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissionDeniedException extends RuntimeException {

    private final String resource;
    private final String action;
    private final String userId;
    private final String tenantId;

    /**
     * Create permission denied exception with full context
     */
    public PermissionDeniedException(String message, String resource, String action, String userId, String tenantId) {
        super(message);
        this.resource = resource;
        this.action = action;
        this.userId = userId;
        this.tenantId = tenantId;
    }

    /**
     * Create permission denied exception with simple message
     */
    public PermissionDeniedException(String message) {
        super(message);
        this.resource = null;
        this.action = null;
        this.userId = null;
        this.tenantId = null;
    }

    /**
     * Create permission denied exception with message and cause
     */
    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
        this.resource = null;
        this.action = null;
        this.userId = null;
        this.tenantId = null;
    }

    /**
     * Factory method for creating exception from permission check context
     */
    public static PermissionDeniedException forPermissionCheck(
            String userId,
            String username,
            String tenantId,
            String resource,
            String action
    ) {
        String message = String.format(
            "Access denied: User '%s' (ID: %s) lacks permission '%s' on resource '%s' in tenant '%s'",
            username, userId, action, resource, tenantId
        );
        return new PermissionDeniedException(message, resource, action, userId, tenantId);
    }

    /**
     * Factory method for tenant isolation violation
     */
    public static PermissionDeniedException forTenantViolation(
            String userId,
            String username,
            String userTenant,
            String resourceTenant
    ) {
        String message = String.format(
            "Tenant isolation violation: User '%s' (tenant: %s) attempted to access resource from tenant '%s'",
            username, userTenant, resourceTenant
        );
        return new PermissionDeniedException(message, null, "cross-tenant-access", userId, userTenant);
    }

    /**
     * Factory method for missing authentication
     */
    public static PermissionDeniedException forMissingAuthentication() {
        return new PermissionDeniedException(
            "Authentication required: No authenticated user found in security context"
        );
    }

    @Override
    public String toString() {
        if (resource != null && action != null) {
            return String.format(
                "PermissionDeniedException{message='%s', resource='%s', action='%s', userId='%s', tenantId='%s'}",
                getMessage(), resource, action, userId, tenantId
            );
        }
        return String.format("PermissionDeniedException{message='%s'}", getMessage());
    }
}
