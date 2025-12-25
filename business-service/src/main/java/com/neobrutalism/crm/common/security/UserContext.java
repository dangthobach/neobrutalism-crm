package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility service to access current authenticated user's context
 * Provides safe access to user ID and organization ID from Spring Security context
 */
@Component
public class UserContext {

    /**
     * Get current authenticated user's ID
     *
     * @return Optional containing user ID if authenticated, empty otherwise
     */
    public Optional<String> getCurrentUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .filter(name -> !"anonymousUser".equals(name));
    }

    /**
     * Get current authenticated user's organization ID
     *
     * @return Optional containing organization ID if available, empty otherwise
     */
    public Optional<String> getCurrentOrganizationId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof UserPrincipal)
            .map(principal -> {
                UserPrincipal userPrincipal = (UserPrincipal) principal;
                return userPrincipal.getOrganizationId() != null ?
                    userPrincipal.getOrganizationId().toString() : null;
            });
    }

    /**
     * Get current user ID or throw exception if not authenticated
     *
     * @return User ID
     * @throws UnauthorizedException if no authenticated user
     */
    public String getCurrentUserIdOrThrow() {
        return getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("No authenticated user in context"));
    }

    /**
     * Get current organization ID or throw exception if not available
     *
     * @return Organization ID
     * @throws UnauthorizedException if no organization context
     */
    public String getCurrentOrganizationIdOrThrow() {
        return getCurrentOrganizationId()
            .orElseThrow(() -> new UnauthorizedException("No organization context available"));
    }

    /**
     * Check if current user is authenticated
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return getCurrentUserId().isPresent();
    }

    /**
     * Get current authentication object
     *
     * @return Optional containing Authentication if available
     */
    public Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated);
    }
}
