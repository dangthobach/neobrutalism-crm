package com.neobrutalism.crm.domain.permission.listener;

import com.neobrutalism.crm.domain.permission.event.PermissionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Permission Cache Invalidation Listener
 *
 * Listens for PermissionChangedEvent and invalidates relevant caches.
 * This ensures that permission checks always use the latest policies.
 *
 * Cache Invalidation Strategy:
 * - ROLE_ASSIGNED/REMOVED: Invalidate user's permission cache
 * - POLICY_ADDED/DELETED: Invalidate role's permission cache
 * - HIERARCHY_CHANGED: Invalidate all permission caches
 * - BULK_UPDATE: Invalidate all permission caches
 * - POLICY_RELOAD: Invalidate all permission caches
 *
 * Performance Considerations:
 * - Async execution to avoid blocking permission operations
 * - Selective cache invalidation when possible
 * - Full cache clear only for bulk operations
 *
 * @author Neobrutalism CRM Team
 */
@Component
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheInvalidationListener {

    private final CacheManager cacheManager;

    /**
     * Cache names used by permission service
     */
    private static final String PERMISSION_CACHE = "permissions";
    private static final String ROLE_PERMISSION_CACHE = "rolePermissions";
    private static final String USER_ROLES_CACHE = "userRoles";

    /**
     * Handle permission changed events
     * Executes asynchronously to avoid blocking the permission operation
     */
    @EventListener
    @Async
    public void handlePermissionChanged(PermissionChangedEvent event) {
        try {
            log.debug("Processing permission changed event: {}", event.getChangeType());

            switch (event.getChangeType()) {
                case ROLE_ASSIGNED, ROLE_REMOVED -> invalidateUserCaches(event);
                case POLICY_ADDED, POLICY_DELETED -> invalidateRoleCaches(event);
                case HIERARCHY_CHANGED, BULK_UPDATE, POLICY_RELOAD -> invalidateAllCaches(event);
            }

            log.info("Cache invalidation completed for event: {} (tenant: {}, user: {}, role: {})",
                    event.getChangeType(),
                    event.getTenantId(),
                    event.getUserId(),
                    event.getRoleCode());

        } catch (Exception e) {
            log.error("Failed to invalidate cache for permission changed event: {}", event, e);
            // Don't rethrow - cache invalidation failure shouldn't break the operation
        }
    }

    /**
     * Invalidate caches for a specific user
     * Called when user's roles are modified
     */
    private void invalidateUserCaches(PermissionChangedEvent event) {
        if (event.getUserId() == null) {
            log.warn("Cannot invalidate user cache - userId is null");
            return;
        }

        String userId = event.getUserId().toString();
        String tenantId = event.getTenantId();

        // Invalidate user's permission cache
        evictCacheEntry(PERMISSION_CACHE, generateUserCacheKey(userId, tenantId));

        // Invalidate user's roles cache
        evictCacheEntry(USER_ROLES_CACHE, generateUserRolesCacheKey(userId, tenantId));

        log.debug("Invalidated caches for user: {} in tenant: {}", userId, tenantId);
    }

    /**
     * Invalidate caches for a specific role
     * Called when role's policies are modified
     */
    private void invalidateRoleCaches(PermissionChangedEvent event) {
        if (event.getRoleCode() == null) {
            log.warn("Cannot invalidate role cache - roleCode is null");
            return;
        }

        String roleCode = event.getRoleCode();
        String tenantId = event.getTenantId();

        // Invalidate role's permission cache
        evictCacheEntry(ROLE_PERMISSION_CACHE, generateRoleCacheKey(roleCode, tenantId));

        // Also invalidate all users with this role (since their effective permissions changed)
        // For performance, we clear the entire permission cache for this tenant
        clearCacheForTenant(PERMISSION_CACHE, tenantId);

        log.debug("Invalidated caches for role: {} in tenant: {}", roleCode, tenantId);
    }

    /**
     * Invalidate all permission caches
     * Called for bulk operations and hierarchy changes
     */
    private void invalidateAllCaches(PermissionChangedEvent event) {
        log.info("Invalidating all permission caches due to: {}", event.getChangeType());

        clearCache(PERMISSION_CACHE);
        clearCache(ROLE_PERMISSION_CACHE);
        clearCache(USER_ROLES_CACHE);

        log.info("All permission caches invalidated");
    }

    /**
     * Evict a specific cache entry
     */
    private void evictCacheEntry(String cacheName, String key) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.trace("Evicted cache entry: {}:{}", cacheName, key);
            } else {
                log.warn("Cache not found: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to evict cache entry: {}:{}", cacheName, key, e);
        }
    }

    /**
     * Clear entire cache
     */
    private void clearCache(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            } else {
                log.warn("Cache not found: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to clear cache: {}", cacheName, e);
        }
    }

    /**
     * Clear cache entries for a specific tenant
     * This is a workaround since Spring Cache doesn't support pattern-based eviction
     */
    private void clearCacheForTenant(String cacheName, String tenantId) {
        // For Redis, we could use pattern-based deletion
        // For now, clear the entire cache (simpler and safer)
        clearCache(cacheName);
    }

    /**
     * Generate cache key for user permissions
     */
    private String generateUserCacheKey(String userId, String tenantId) {
        return String.format("%s:%s", tenantId, userId);
    }

    /**
     * Generate cache key for user roles
     */
    private String generateUserRolesCacheKey(String userId, String tenantId) {
        return String.format("%s:%s", tenantId, userId);
    }

    /**
     * Generate cache key for role permissions
     */
    private String generateRoleCacheKey(String roleCode, String tenantId) {
        return String.format("%s:%s", tenantId, roleCode);
    }
}
