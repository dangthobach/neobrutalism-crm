package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter
 * Intercepts requests, validates JWT tokens, and checks Casbin permissions
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSessionService userSessionService;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Extract user information from token
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);
                String tokenType = jwtTokenProvider.getTokenTypeFromToken(jwt);

                // Only process access tokens, not refresh tokens
                if ("access".equals(tokenType)) {
                    // Set tenant context for multi-tenancy
                    TenantContext.setCurrentTenant(tenantId);

                    // Load complete user principal with roles and permissions (cached)
                    UserPrincipal userPrincipal = userSessionService.buildUserPrincipal(userId, tenantId);

                    // Check Casbin permission for this request
                    String requestUri = request.getRequestURI();
                    String method = request.getMethod();

                    boolean hasPermission = checkPermission(userPrincipal, tenantId, requestUri, method);

                    if (!hasPermission) {
                        log.warn("User {} denied access to {} {} (tenant: {})",
                                username, method, requestUri, tenantId);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"You do not have permission to access this resource\"}");
                        return;
                    }

                    // Create authentication with full authorities
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    userPrincipal.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {} (tenant: {}, roles: {})",
                            username, tenantId, userPrincipal.getRoles());
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear tenant context after request
            TenantContext.clear();
        }
    }

    /**
     * Check permission using Casbin
     */
    private boolean checkPermission(UserPrincipal userPrincipal, String tenantId, String resource, String action) {
        // Skip permission check for public endpoints
        if (isPublicEndpoint(resource)) {
            return true;
        }

        // Check each role's permissions
        for (String role : userPrincipal.getRoles()) {
            String subject = "ROLE_" + role;
            boolean allowed = permissionService.hasPermission(subject, tenantId, resource, action);
            if (allowed) {
                log.debug("Permission granted for {} to {} {} (tenant: {})", subject, action, resource, tenantId);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if endpoint is public (doesn't require permission check)
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/auth/") ||
               uri.startsWith("/h2-console") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.equals("/actuator/health") ||
               uri.startsWith("/api/public/");
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
