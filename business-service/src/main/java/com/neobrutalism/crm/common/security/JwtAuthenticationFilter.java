package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.common.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TokenBlacklistService tokenBlacklistService;
    
    // Optional: Only available when casbin.enabled=true
    @Autowired(required = false)
    private PermissionService permissionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // ⭐ OPTIMIZATION: Skip JWT validation if already authenticated by Gateway
            // GatewayAuthenticationFilter runs first and sets SecurityContext
            if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                log.debug("✅ User already authenticated by Gateway, skipping JWT validation");
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Check if token is blacklisted
                if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                    log.warn("Blocked blacklisted token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extract user information from token
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);
                String tokenType = jwtTokenProvider.getTokenTypeFromToken(jwt);

                // Check if all user's tokens are blacklisted (e.g., password changed)
                if (tokenBlacklistService.areUserTokensBlacklisted(userId.toString())) {
                    log.warn("Blocked token for user with blacklisted tokens: {}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Only process access tokens, not refresh tokens
                if ("access".equals(tokenType)) {
                    // Set tenant context for multi-tenancy
                    TenantContext.setCurrentTenant(tenantId);

                    // Load complete user principal with roles and permissions (cached)
                    UserPrincipal userPrincipal = userSessionService.buildUserPrincipal(userId, tenantId);

                    // Note: Permission checking is handled by CasbinAuthorizationFilter
                    // This filter only authenticates the user and sets SecurityContext

                    // Create authentication with full authorities
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    userPrincipal.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Populate DataScopeContext from UserPrincipal for data scope enforcement
                    DataScopeContext dataScopeContext = DataScopeContext.builder()
                            .userId(userPrincipal.getId())
                            .tenantId(userPrincipal.getTenantId())
                            .dataScope(userPrincipal.getDataScope())
                            .branchId(userPrincipal.getBranchId())
                            .accessibleBranchIds(userPrincipal.getAccessibleBranchIds())
                            .build();
                    DataScopeContext.set(dataScopeContext);

                    log.debug("Set authentication for user: {} (tenant: {}, roles: {})",
                            username, tenantId, userPrincipal.getRoles());
                    log.debug("DataScopeContext populated: userId={}, scope={}, branchId={}, accessibleBranches={}",
                            userPrincipal.getId(),
                            userPrincipal.getDataScope(),
                            userPrincipal.getBranchId(),
                            userPrincipal.getAccessibleBranchIds() != null ? userPrincipal.getAccessibleBranchIds().size() : 0);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear contexts after request
            TenantContext.clear();
            DataScopeContext.clear();
        }
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
