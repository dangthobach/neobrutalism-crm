package com.neobrutalism.crm.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cache Invalidation Service
 * Handles cache invalidation strategies for role/permission changes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final CacheManager cacheManager;

    /**
     * Invalidate all menu-related caches
     * Called when menus are created, updated, or deleted
     */
    public void invalidateMenuCaches() {
        log.info("Invalidating all menu caches");
        evictCache("menuTree");
        evictCache("userMenus");
        log.info("Menu caches invalidated successfully");
    }

    /**
     * Invalidate caches for a specific user
     * Called when user's roles or groups change
     */
    public void invalidateUserCaches(UUID userId) {
        log.info("Invalidating caches for user: {}", userId);
        evictCacheKey("userMenus", userId.toString());
        evictCacheKey("userRoles", userId.toString());
        log.info("User caches invalidated successfully for user: {}", userId);
    }

    /**
     * Invalidate role-related caches
     * Called when role permissions are modified
     */
    public void invalidateRoleCaches(UUID roleId) {
        log.info("Invalidating caches for role: {}", roleId);
        evictCacheKey("rolePermissions", roleId.toString());
        evictCacheKey("roleByCode", roleId.toString());

        // Also invalidate all user menus since role permissions changed
        evictCache("userMenus");

        log.info("Role caches invalidated successfully for role: {}", roleId);
    }

    /**
     * Invalidate caches when role permissions are bulk updated
     */
    public void invalidateRolePermissionCaches(UUID roleId) {
        log.info("Invalidating permission caches for role: {}", roleId);
        evictCacheKey("rolePermissions", roleId.toString());

        // Invalidate all user menus since permissions changed
        evictCache("userMenus");

        log.info("Permission caches invalidated successfully for role: {}", roleId);
    }

    /**
     * Invalidate group-related caches
     * Called when group members or group roles change
     */
    public void invalidateGroupCaches(UUID groupId) {
        log.info("Invalidating caches for group: {}", groupId);
        evictCacheKey("groupMembers", groupId.toString());

        // Also invalidate user menus for all members
        evictCache("userMenus");

        log.info("Group caches invalidated successfully for group: {}", groupId);
    }

    /**
     * Invalidate user lookup caches
     * Called when user is updated
     */
    public void invalidateUserLookupCaches(String username, String email) {
        log.info("Invalidating user lookup caches for username: {}, email: {}", username, email);
        evictCacheKey("userByUsername", username);
        evictCacheKey("userByEmail", email);
        log.info("User lookup caches invalidated successfully");
    }

    /**
     * Invalidate all permission-related caches
     * Nuclear option for major permission system changes
     */
    public void invalidateAllPermissionCaches() {
        log.warn("Invalidating ALL permission caches - this should be rare");
        evictCache("menuTree");
        evictCache("userMenus");
        evictCache("rolePermissions");
        evictCache("userRoles");
        evictCache("groupMembers");
        log.info("All permission caches invalidated successfully");
    }

    /**
     * Evict entire cache by name
     */
    private void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Evicted entire cache: {}", cacheName);
        }
    }

    /**
     * Evict specific key from cache
     */
    private void evictCacheKey(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted key '{}' from cache: {}", key, cacheName);
        }
    }
}
