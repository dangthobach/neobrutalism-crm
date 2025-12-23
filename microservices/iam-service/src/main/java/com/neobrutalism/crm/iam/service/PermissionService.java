package com.neobrutalism.crm.iam.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.neobrutalism.crm.iam.config.CacheConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.casbin.jcasbin.main.Enforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission Service with Multi-tier Caching
 *
 * Permission check flow:
 * 1. Check L1 cache (Caffeine) - ~0.001ms
 * 2. Check L2 cache (Redis) - ~0.5ms
 * 3. Query database via Casbin - ~5ms
 * 4. Cache result in L1 and L2
 *
 * Performance targets:
 * - Cache hit rate > 95%
 * - P95 latency < 5ms
 * - P99 latency < 50ms
 * - Support 10,000 requests/second
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final Enforcer enforcer;
    private final Cache<String, Boolean> permissionCheckCache;
    private final Cache<CacheConfig.UserPermissionContext, CacheConfig.UserPermissionContext> userPermissionsCache;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    public PermissionService(
            Enforcer enforcer,
            @Qualifier("permissionCheckCache") Cache<String, Boolean> permissionCheckCache,
            @Qualifier("userPermissionsCache") Cache<CacheConfig.UserPermissionContext, CacheConfig.UserPermissionContext> userPermissionsCache,
            ReactiveRedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry
    ) {
        this.enforcer = enforcer;
        this.permissionCheckCache = permissionCheckCache;
        this.userPermissionsCache = userPermissionsCache;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Check if user has permission to perform action on resource
     *
     * @param userId User ID
     * @param tenantId Tenant ID (domain)
     * @param resource Resource path (e.g., /api/customers/123)
     * @param action HTTP method or action (e.g., GET, POST, READ, WRITE)
     * @return Mono<Boolean> true if allowed, false otherwise
     */
    public Mono<Boolean> checkPermission(String userId, String tenantId, String resource, String action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String cacheKey = CacheConfig.permissionCacheKey(userId, tenantId, resource, action);

        // L1: Check Caffeine cache
        Boolean cachedResult = permissionCheckCache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            sample.stop(meterRegistry.timer("permission.check", "cache", "l1", "result", "hit"));
            log.trace("L1 cache HIT for permission check: {}", cacheKey);
            return Mono.just(cachedResult);
        }

        // L2: Check Redis cache
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .map(value -> {
                    Boolean result = (Boolean) value;
                    // Store in L1 cache
                    permissionCheckCache.put(cacheKey, result);
                    sample.stop(meterRegistry.timer("permission.check", "cache", "l2", "result", "hit"));
                    log.trace("L2 cache HIT for permission check: {}", cacheKey);
                    return result;
                })
                .switchIfEmpty(
                        // L3: Query Casbin enforcer
                        Mono.fromCallable(() -> {
                            boolean result = enforcer.enforce(userId, tenantId, resource, action);

                            // Cache in L1 and L2
                            permissionCheckCache.put(cacheKey, result);
                            redisTemplate.opsForValue()
                                    .set(cacheKey, result, Duration.ofSeconds(300))
                                    .subscribe();

                            sample.stop(meterRegistry.timer("permission.check", "cache", "l3", "result", result ? "allowed" : "denied"));
                            log.debug("L3 database query for permission check: {} -> {}", cacheKey, result);
                            return result;
                        })
                );
    }

    /**
     * Get all permissions for a user in a tenant
     * Returns a map of resource patterns to allowed actions
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Mono<Map<String, Set<String>>> resource -> actions
     */
    public Mono<Map<String, Set<String>>> getUserPermissions(String userId, String tenantId) {
        String cacheKey = CacheConfig.UserPermissionContext.cacheKey(userId, tenantId);

        // Check L1 cache
        CacheConfig.UserPermissionContext cachedContext = userPermissionsCache.getIfPresent(cacheKey);
        if (cachedContext != null && !cachedContext.isExpired(60)) {
            log.trace("L1 cache HIT for user permissions: {}", cacheKey);
            return Mono.just(cachedContext.permissions());
        }

        // L2: Check Redis
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .map(value -> {
                    CacheConfig.UserPermissionContext context = (CacheConfig.UserPermissionContext) value;
                    // Store in L1
                    userPermissionsCache.put(cacheKey, context);
                    log.trace("L2 cache HIT for user permissions: {}", cacheKey);
                    return context.permissions();
                })
                .switchIfEmpty(
                        // L3: Query database
                        Mono.fromCallable(() -> {
                            Map<String, Set<String>> permissions = loadUserPermissionsFromDatabase(userId, tenantId);
                            Set<String> roles = loadUserRolesFromDatabase(userId, tenantId);

                            CacheConfig.UserPermissionContext context = new CacheConfig.UserPermissionContext(
                                    userId, tenantId, roles, permissions, System.currentTimeMillis()
                            );

                            // Cache in L1 and L2
                            userPermissionsCache.put(cacheKey, context);
                            redisTemplate.opsForValue()
                                    .set(cacheKey, context, Duration.ofSeconds(300))
                                    .subscribe();

                            log.debug("Loaded user permissions from database: {}", cacheKey);
                            return permissions;
                        })
                );
    }

    /**
     * Add a role to a user in a tenant
     */
    public Mono<Boolean> addRoleToUser(String userId, String roleId, String tenantId) {
        return Mono.fromCallable(() -> {
            boolean result = enforcer.addGroupingPolicy(userId, roleId, tenantId);
            if (result) {
                invalidateUserCache(userId, tenantId);
                log.info("Added role {} to user {} in tenant {}", roleId, userId, tenantId);
            }
            return result;
        });
    }

    /**
     * Remove a role from a user in a tenant
     */
    public Mono<Boolean> removeRoleFromUser(String userId, String roleId, String tenantId) {
        return Mono.fromCallable(() -> {
            boolean result = enforcer.removeGroupingPolicy(userId, roleId, tenantId);
            if (result) {
                invalidateUserCache(userId, tenantId);
                log.info("Removed role {} from user {} in tenant {}", roleId, userId, tenantId);
            }
            return result;
        });
    }

    /**
     * Add a permission policy
     */
    public Mono<Boolean> addPolicy(String roleId, String tenantId, String resource, String action) {
        return Mono.fromCallable(() -> {
            boolean result = enforcer.addPolicy(roleId, tenantId, resource, action);
            if (result) {
                invalidatePolicyCache(tenantId);
                log.info("Added policy: role={}, tenant={}, resource={}, action={}",
                        roleId, tenantId, resource, action);
            }
            return result;
        });
    }

    /**
     * Remove a permission policy
     */
    public Mono<Boolean> removePolicy(String roleId, String tenantId, String resource, String action) {
        return Mono.fromCallable(() -> {
            boolean result = enforcer.removePolicy(roleId, tenantId, resource, action);
            if (result) {
                invalidatePolicyCache(tenantId);
                log.info("Removed policy: role={}, tenant={}, resource={}, action={}",
                        roleId, tenantId, resource, action);
            }
            return result;
        });
    }

    /**
     * Get all roles for a user in a tenant
     */
    public Mono<Set<String>> getUserRoles(String userId, String tenantId) {
        return Mono.fromCallable(() -> loadUserRolesFromDatabase(userId, tenantId));
    }

    /**
     * Load user permissions from database via Casbin
     */
    private Map<String, Set<String>> loadUserPermissionsFromDatabase(String userId, String tenantId) {
        // Get all permissions for the user through their roles
        List<List<String>> permissions = enforcer.getImplicitPermissionsForUser(userId, tenantId);

        Map<String, Set<String>> permissionMap = new HashMap<>();
        for (List<String> permission : permissions) {
            if (permission.size() >= 4) {
                String resource = permission.get(2);
                String action = permission.get(3);
                permissionMap.computeIfAbsent(resource, k -> new HashSet<>()).add(action);
            }
        }

        return permissionMap;
    }

    /**
     * Load user roles from database via Casbin
     */
    private Set<String> loadUserRolesFromDatabase(String userId, String tenantId) {
        List<String> roles = enforcer.getRolesForUserInDomain(userId, tenantId);
        return new HashSet<>(roles);
    }

    /**
     * Invalidate all caches for a user
     */
    private void invalidateUserCache(String userId, String tenantId) {
        String cacheKey = CacheConfig.UserPermissionContext.cacheKey(userId, tenantId);

        // Clear L1 cache
        userPermissionsCache.invalidate(cacheKey);

        // Clear L2 cache
        redisTemplate.delete(cacheKey).subscribe();

        // Clear permission check cache (all entries for this user)
        permissionCheckCache.asMap().keySet().stream()
                .filter(key -> key.startsWith(String.format("user:%s:tenant:%s:", userId, tenantId)))
                .forEach(permissionCheckCache::invalidate);

        log.debug("Invalidated cache for user {} in tenant {}", userId, tenantId);
    }

    /**
     * Invalidate policy caches for a tenant
     */
    private void invalidatePolicyCache(String tenantId) {
        // Clear all permission check caches for this tenant
        permissionCheckCache.asMap().keySet().stream()
                .filter(key -> key.contains(String.format(":tenant:%s:", tenantId)))
                .forEach(permissionCheckCache::invalidate);

        log.debug("Invalidated policy cache for tenant {}", tenantId);
    }
}
