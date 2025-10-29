package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.config.CacheConfig;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import com.neobrutalism.crm.domain.userrole.model.UserRole;
import com.neobrutalism.crm.domain.userrole.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserSessionService - Manages user session data with caching
 * Caches user information, roles, and permissions to reduce database queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    /**
     * Get user by ID with caching
     */
    @Cacheable(value = CacheConfig.USER_CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public Optional<User> getUserById(UUID userId) {
        log.debug("Loading user from database: {}", userId);
        return userRepository.findByIdAndDeletedFalse(userId);
    }

    /**
     * Get user roles with caching
     */
    @Cacheable(value = CacheConfig.USER_ROLES_CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(UUID userId) {
        log.debug("Loading user roles from database: {}", userId);

        // Get all active user-role mappings
        List<UserRole> userRoles = userRoleRepository.findByUserIdAndIsActiveTrue(userId);

        if (userRoles.isEmpty()) {
            return new HashSet<>();
        }

        // Filter by expiration and get role IDs
        Set<UUID> roleIds = userRoles.stream()
                .filter(ur -> ur.getExpiresAt() == null || ur.getExpiresAt().isAfter(Instant.now()))
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }

        // Load roles
        List<Role> roles = roleRepository.findAllById(roleIds);

        return roles.stream()
                .filter(role -> !role.getDeleted())
                .map(Role::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * Get user permissions from Casbin with caching
     */
    @Cacheable(value = CacheConfig.USER_PERMISSIONS_CACHE, key = "#userId + '_' + #tenantId")
    public Set<String> getUserPermissions(UUID userId, String tenantId) {
        log.debug("Loading user permissions from Casbin: {} (tenant: {})", userId, tenantId);

        Set<String> roles = getUserRoles(userId);
        Set<String> allPermissions = new HashSet<>();

        // Get permissions for each role from Casbin
        for (String roleCode : roles) {
            List<List<String>> permissions = permissionService.getPermissionsForRole(roleCode, tenantId);

            // Extract resource and action from permissions
            for (List<String> permission : permissions) {
                if (permission.size() >= 4) {
                    // Permission format: [roleCode, tenant, resource, action]
                    String resource = permission.get(2);
                    String action = permission.get(3);
                    allPermissions.add(resource + ":" + action);
                }
            }
        }

        return allPermissions;
    }

    /**
     * Build complete UserPrincipal with cached data
     */
    @Transactional(readOnly = true)
    public UserPrincipal buildUserPrincipal(UUID userId, String tenantId) {
        User user = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Set<String> roles = getUserRoles(userId);
        Set<String> permissions = getUserPermissions(userId, tenantId);

        return UserPrincipal.create(user, roles, permissions);
    }

    /**
     * Clear user cache when user data changes
     */
    @CacheEvict(value = {
            CacheConfig.USER_CACHE,
            CacheConfig.USER_ROLES_CACHE,
            CacheConfig.USER_PERMISSIONS_CACHE
    }, key = "#userId")
    public void evictUserCache(UUID userId) {
        log.debug("Evicting user cache: {}", userId);
    }

    /**
     * Clear all user roles cache (useful when roles are modified)
     */
    @CacheEvict(value = CacheConfig.USER_ROLES_CACHE, allEntries = true)
    public void evictAllRolesCache() {
        log.debug("Evicting all user roles cache");
    }

    /**
     * Clear all user permissions cache (useful when permissions are modified)
     */
    @CacheEvict(value = CacheConfig.USER_PERMISSIONS_CACHE, allEntries = true)
    public void evictAllPermissionsCache() {
        log.debug("Evicting all user permissions cache");
    }

    /**
     * Clear all caches for a user
     */
    public void clearUserSession(UUID userId) {
        evictUserCache(userId);
    }
}
