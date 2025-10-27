package com.neobrutalism.crm.common.security.aspect;

import com.neobrutalism.crm.common.enums.PermissionType;
import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.security.annotation.RequirePermission;
import com.neobrutalism.crm.common.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Permission Aspect
 * AOP interceptor for @RequirePermission annotation
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Before("@annotation(com.neobrutalism.crm.common.security.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // Get authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("User not authenticated");
        }

        // Get user ID
        UUID userId = (UUID) authentication.getPrincipal();

        // Get annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        String resource = annotation.resource();
        PermissionType permission = annotation.permission();
        String action = annotation.action();
        boolean checkTenant = annotation.checkTenant();

        // Check tenant isolation if required
        if (checkTenant) {
            String currentTenant = TenantContext.getCurrentTenant();
            if (currentTenant == null) {
                throw new BusinessException("No tenant context set");
            }
        }

        // Check permission
        boolean hasPermission = permissionService.hasPermission(userId, resource, permission, action);

        if (!hasPermission) {
            String message = String.format(
                    "Access denied. User %s does not have %s permission for resource %s",
                    userId, permission, resource
            );
            log.warn(message);
            throw new BusinessException(message);
        }

        log.debug("Permission check passed for user {} on resource {} with permission {}",
                userId, resource, permission);
    }
}
