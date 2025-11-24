package com.neobrutalism.crm.config.security;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Casbin Authorization Filter
 * Intercepts all requests and checks permissions using jCasbin
 * 
 * Features:
 * - Dynamic authorization based on role-menu mappings
 * - Multi-tenant support using domain model
 * - Skip authentication endpoints
 * - HTTP method to action mapping (GET->read, POST->create, etc.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CasbinAuthorizationFilter extends OncePerRequestFilter {

    private final Enforcer enforcer;

    // Endpoints that don't require authorization
    private static final List<String> SKIP_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/public",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/error"
    );

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip authorization for public endpoints
        if (shouldSkipAuthorization(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access attempt to: {} by unauthenticated user", requestPath);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String username = authentication.getName();
        String domain = TenantContext.getCurrentTenant(); // Multi-tenant support
        String action = mapHttpMethodToAction(method);

        // Check permission using Casbin
        boolean hasPermission = enforcer.enforce(username, domain, requestPath, action);

        if (!hasPermission) {
            log.warn("Access denied: user={}, domain={}, resource={}, action={}", 
                username, domain, requestPath, action);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                "You don't have permission to perform this action");
            return;
        }

        log.debug("Access granted: user={}, resource={}, action={}", 
            username, requestPath, action);
        
        filterChain.doFilter(request, response);
    }

    /**
     * Check if path should skip authorization
     */
    private boolean shouldSkipAuthorization(String path) {
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Map HTTP method to Casbin action
     */
    private String mapHttpMethodToAction(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> "read";
            case "POST" -> "create";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> "read";
        };
    }
}
