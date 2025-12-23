package com.neobrutalism.crm.config.security;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
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
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class CasbinAuthorizationFilter extends OncePerRequestFilter {

    private final Enforcer enforcer;
    private final Environment environment;

    // Endpoints that don't require authorization
    private static final List<String> SKIP_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/public",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/error",
        "/ws",           // WebSocket handshake endpoints
        "/app",          // WebSocket application messages
        "/topic",        // WebSocket topic subscriptions
        "/queue"         // WebSocket queue subscriptions
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
        String domain = TenantContext.getCurrentTenantOrDefault(); // Multi-tenant support, use default if not set
        String action = mapHttpMethodToAction(method);

        // Try to get UserPrincipal to check roles
        UserPrincipal userPrincipal = null;
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            userPrincipal = (UserPrincipal) authentication.getPrincipal();
        }

        // Check permission using Casbin
        // First try with roles (preferred), then fallback to username
        boolean hasPermission = false;

        if (userPrincipal != null && !userPrincipal.getRoles().isEmpty()) {
            // Check each role's permissions
            for (String role : userPrincipal.getRoles()) {
                String subject = "ROLE_" + role;
                hasPermission = enforcer.enforce(subject, domain, requestPath, action);
                if (hasPermission) {
                    log.debug("Permission granted via role {} for {} {} (tenant: {})", 
                        subject, action, requestPath, domain);
                    break;
                }
            }
        }

        // Fallback: Check with username (for backward compatibility or user-specific policies)
        if (!hasPermission) {
            hasPermission = enforcer.enforce(username, domain, requestPath, action);
            if (hasPermission) {
                log.debug("Permission granted via username {} for {} {} (tenant: {})", 
                    username, action, requestPath, domain);
            }
        }

        // Fallback: In dev mode, if no policies exist (all enforce return false), allow authenticated users
        // This helps during development when policies haven't been set up yet
        if (!hasPermission) {
            // Check if we're in dev profile
            boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
            
            // Check if there are any policies at all by trying to get policies
            // If no policies exist, enforcer.getPolicy() will return empty list
            List<List<String>> allPolicies = enforcer.getPolicy();
            
            if (isDevProfile && allPolicies.isEmpty()) {
                log.debug("Dev mode: No Casbin policies found, allowing authenticated user: {} (tenant: {})", 
                    username, domain);
                hasPermission = true; // Allow authenticated users when no policies exist in dev mode
            } else if (isDevProfile && userPrincipal != null && !userPrincipal.getRoles().isEmpty()) {
                // In dev mode, if user has roles but no matching policies, still allow
                // This is useful when policies are being set up
                log.debug("Dev mode: User has roles but no matching policies, allowing: {} (tenant: {}, roles: {})", 
                    username, domain, userPrincipal.getRoles());
                hasPermission = true;
            }
        }

        if (!hasPermission) {
            log.warn("Access denied: user={}, roles={}, domain={}, resource={}, action={}", 
                username, 
                userPrincipal != null ? userPrincipal.getRoles() : "N/A",
                domain, requestPath, action);
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
