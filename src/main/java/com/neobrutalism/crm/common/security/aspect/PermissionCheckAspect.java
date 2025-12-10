package com.neobrutalism.crm.common.security.aspect;

import com.neobrutalism.crm.common.enums.PermissionType;
import com.neobrutalism.crm.common.exception.PermissionDeniedException;
import com.neobrutalism.crm.common.security.PermissionService;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.common.security.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect for enforcing @RequirePermission annotation
 *
 * This aspect intercepts methods annotated with @RequirePermission and checks
 * if the current user has the required permission before allowing execution.
 *
 * Execution Order:
 * - Runs after @Transactional (Order = HIGHEST_PRECEDENCE + 1)
 * - Permission check happens BEFORE method execution
 * - Throws PermissionDeniedException if check fails
 *
 * Features:
 * - Automatic permission checking based on annotation
 * - Tenant isolation enforcement
 * - Resource-based access control
 * - Action-level granularity (READ, WRITE, DELETE, EXECUTE)
 * - Integration with Casbin for policy evaluation
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // After @Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckAspect {

    @Autowired(required = false)
    private final PermissionService permissionService;

    /**
     * Intercept methods annotated with @RequirePermission
     * Checks permission BEFORE method execution
     */
    @Before("@annotation(com.neobrutalism.crm.common.security.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // Skip if PermissionService is not available (casbin.enabled=false)
        if (permissionService == null) {
            log.debug("PermissionService not available - skipping permission check");
            return;
        }

        // Get the method and annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (annotation == null) {
            log.warn("@RequirePermission annotation not found on method: {}", method.getName());
            return;
        }

        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found for permission check on method: {}", method.getName());
            throw PermissionDeniedException.forMissingAuthentication();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            log.warn("Principal is not UserPrincipal: {} - skipping permission check", principal.getClass().getSimpleName());
            return;
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        // Extract annotation values
        String resource = annotation.resource();
        PermissionType permissionType = annotation.permission();
        String action = annotation.action();
        boolean checkTenant = annotation.checkTenant();

        // Convert PermissionType to action string if custom action not provided
        if (action == null || action.isEmpty()) {
            action = mapPermissionTypeToAction(permissionType);
        }

        // Build resource path (e.g., /api/customers)
        String resourcePath = buildResourcePath(resource);

        // Log permission check
        log.debug("Permission check: user={}, tenant={}, resource={}, action={}, method={}",
                userPrincipal.getUsername(),
                userPrincipal.getTenantId(),
                resourcePath,
                action,
                method.getName());

        // Check permission using PermissionService (Casbin)
        boolean hasPermission = permissionService.hasPermission(
                userPrincipal.getId(),
                userPrincipal.getTenantId(),
                resourcePath,
                action
        );

        if (!hasPermission) {
            log.warn("Permission denied: user={}, resource={}, action={}, method={}",
                    userPrincipal.getUsername(),
                    resourcePath,
                    action,
                    method.getName());

            throw PermissionDeniedException.forPermissionCheck(
                    userPrincipal.getId().toString(),
                    userPrincipal.getUsername(),
                    userPrincipal.getTenantId(),
                    resourcePath,
                    action
            );
        }

        // Optional: Check tenant isolation for methods with parameters
        if (checkTenant) {
            checkTenantIsolation(joinPoint, userPrincipal);
        }

        log.debug("Permission check passed: user={}, resource={}, action={}",
                userPrincipal.getUsername(),
                resourcePath,
                action);
    }

    /**
     * Map PermissionType enum to HTTP action string
     */
    private String mapPermissionTypeToAction(PermissionType permissionType) {
        switch (permissionType) {
            case READ:
                return "GET";
            case WRITE:
                return "POST"; // or PUT for updates
            case DELETE:
                return "DELETE";
            case EXECUTE:
                return "POST"; // For execute actions
            default:
                return "GET";
        }
    }

    /**
     * Build resource path from resource name
     * Converts "CUSTOMER" -> "/api/customers"
     * Converts "/api/users" -> "/api/users" (already a path)
     */
    private String buildResourcePath(String resource) {
        // If already a path (starts with /), return as-is
        if (resource.startsWith("/")) {
            return resource;
        }

        // Convert resource name to path
        // CUSTOMER -> /api/customers
        // USER -> /api/users
        String lowercaseResource = resource.toLowerCase();

        // Handle plural forms
        if (!lowercaseResource.endsWith("s") &&
            !lowercaseResource.endsWith("data") &&
            !lowercaseResource.endsWith("info")) {
            lowercaseResource = lowercaseResource + "s";
        }

        return "/api/" + lowercaseResource;
    }

    /**
     * Check tenant isolation by examining method parameters
     * Ensures user can only access resources from their tenant
     */
    private void checkTenantIsolation(JoinPoint joinPoint, UserPrincipal userPrincipal) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        // Look for tenantId parameter
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }

            // Check if parameter is named "tenantId" or "tenant"
            if (i < parameterNames.length) {
                String paramName = parameterNames[i].toLowerCase();
                if (paramName.contains("tenant")) {
                    String paramTenantId = args[i].toString();
                    if (!paramTenantId.equals(userPrincipal.getTenantId())) {
                        log.warn("Tenant isolation violation: user tenant={}, parameter tenant={}",
                                userPrincipal.getTenantId(),
                                paramTenantId);

                        throw PermissionDeniedException.forTenantViolation(
                                userPrincipal.getId().toString(),
                                userPrincipal.getUsername(),
                                userPrincipal.getTenantId(),
                                paramTenantId
                        );
                    }
                }
            }

            // Check if argument has getTenantId() method (entity objects)
            try {
                Method getTenantIdMethod = args[i].getClass().getMethod("getTenantId");
                Object tenantIdObj = getTenantIdMethod.invoke(args[i]);
                if (tenantIdObj != null) {
                    String argTenantId = tenantIdObj.toString();
                    if (!argTenantId.equals(userPrincipal.getTenantId())) {
                        log.warn("Tenant isolation violation in entity: user tenant={}, entity tenant={}",
                                userPrincipal.getTenantId(),
                                argTenantId);

                        throw PermissionDeniedException.forTenantViolation(
                                userPrincipal.getId().toString(),
                                userPrincipal.getUsername(),
                                userPrincipal.getTenantId(),
                                argTenantId
                        );
                    }
                }
            } catch (Exception e) {
                // Method doesn't exist or can't be invoked - skip this parameter
            }
        }
    }

    /**
     * Intercept class-level @RequirePermission annotation
     * Applies to all methods in the class
     */
    @Before("@within(com.neobrutalism.crm.common.security.annotation.RequirePermission) && " +
            "!@annotation(com.neobrutalism.crm.common.security.annotation.RequirePermission)")
    public void checkClassLevelPermission(JoinPoint joinPoint) {
        // Skip if PermissionService is not available
        if (permissionService == null) {
            return;
        }

        // Get class-level annotation
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RequirePermission annotation = targetClass.getAnnotation(RequirePermission.class);

        if (annotation == null) {
            return;
        }

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw PermissionDeniedException.forMissingAuthentication();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            return;
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        // Extract annotation values
        String resource = annotation.resource();
        PermissionType permissionType = annotation.permission();
        String action = annotation.action();

        if (action == null || action.isEmpty()) {
            action = mapPermissionTypeToAction(permissionType);
        }

        String resourcePath = buildResourcePath(resource);

        // Check permission
        boolean hasPermission = permissionService.hasPermission(
                userPrincipal.getId(),
                userPrincipal.getTenantId(),
                resourcePath,
                action
        );

        if (!hasPermission) {
            log.warn("Class-level permission denied: user={}, resource={}, action={}, class={}",
                    userPrincipal.getUsername(),
                    resourcePath,
                    action,
                    targetClass.getSimpleName());

            throw PermissionDeniedException.forPermissionCheck(
                    userPrincipal.getId().toString(),
                    userPrincipal.getUsername(),
                    userPrincipal.getTenantId(),
                    resourcePath,
                    action
            );
        }

        log.debug("Class-level permission check passed: user={}, resource={}, class={}",
                userPrincipal.getUsername(),
                resourcePath,
                targetClass.getSimpleName());
    }
}
