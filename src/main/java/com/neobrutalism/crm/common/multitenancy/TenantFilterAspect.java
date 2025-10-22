package com.neobrutalism.crm.common.multitenancy;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to enable Hibernate tenant filter for repository methods
 * Automatically filters queries by tenant_id
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TenantFilterAspect {

    private final EntityManager entityManager;

    @Around("execution(* com.neobrutalism.crm..repository.*Repository+.*(..))")
    public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get current tenant
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null) {
            try {
                // Enable Hibernate filter - only if filter exists
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);

                log.trace("Enabled tenant filter for repository method: {}", joinPoint.getSignature().getName());
            } catch (org.hibernate.UnknownFilterException e) {
                // Filter doesn't exist for this entity - this is OK
                // Only entities extending TenantAwareEntity have the filter
                log.trace("Tenant filter not applicable for repository method: {}", joinPoint.getSignature().getName());
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            // Disable filter after method execution
            if (tenantId != null) {
                try {
                    Session session = entityManager.unwrap(Session.class);
                    session.disableFilter("tenantFilter");
                } catch (Exception e) {
                    // Ignore - filter may not have been enabled
                }
            }
        }
    }
}
