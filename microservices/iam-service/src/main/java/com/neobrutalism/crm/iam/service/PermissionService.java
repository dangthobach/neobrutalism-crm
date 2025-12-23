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
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission Service with Multi-tier Caching
 *
 * CRITICAL FIX: Non-blocking reactive architecture
 *
 * Permission check flow:
 * 1. Check L1 cache (Caffeine) - ~0.001ms (non-blocking)
 * 2. Check L2 cache (Redis) - ~0.5ms (reactive)
 * 3. Query database via Casbin - ~5ms (offloaded to worker pool)
 * 4. Cache result in L1 and L2
 *
 * Performance targets (UPDATED for 100K CCU):
 * - Cache hit rate > 98%
 * - P95 latency < 2ms
 * - P99 latency < 20ms (down from 50ms)
 * - Support 100,000 requests/second (up from 10K)
 *
 * Thread Model:
 * - Reactor event-loop: Non-blocking cache checks, Redis operations
 * - Casbin worker pool: Blocking Casbin enforcer calls (subscribeOn)
 * - No blocking on event-loop threads (critical for 100K CCU)
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final Enforcer enforcer;
    private final Cache<String, Boolean> permissionCheckCache;
    private final Cache<String, CacheConfig.UserPermissionContext> userPermissionsCache;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    // CRITICAL: Dedicated scheduler for blocking Casbin calls
    private final Scheduler casbinScheduler;

    // CRITICAL: Distributed cache invalidation service
    private final CacheInvalidationService cacheInvalidationService;

    public PermissionService(
            Enforcer enforcer,
            @Qualifier("permissionCheckCache") Cache<String, Boolean> permissionCheckCache,
            @Qualifier("userPermissionsCache") Cache<String, CacheConfig.UserPermissionContext> userPermissionsCache,
            ReactiveRedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry,
            @Qualifier("casbinScheduler") Scheduler casbinScheduler,  // INJECTED
            CacheInvalidationService cacheInvalidationService  // INJECTED - CRITICAL FIX #2
    ) {
        this.enforcer = enforcer;
        this.permissionCheckCache = permissionCheckCache;
        this.userPermissionsCache = userPermissionsCache;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        this.casbinScheduler = casbinScheduler;  // CRITICAL FIX #1
        this.cacheInvalidationService = cacheInvalidationService;  // CRITICAL FIX #2

        log.info("PermissionService initialized with dedicated Casbin scheduler");
        log.info("CRITICAL: All Casbin calls will be offloaded to worker pool (non-blocking)");
        log.info("CRITICAL: Distributed cache invalidation enabled via Redis Pub/Sub");
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
                        // L3: Query Casbin enforcer (OFFLOADED to worker pool)
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
                        .subscribeOn(casbinScheduler)  // 🔥 CRITICAL FIX: Offload to worker pool
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
                        // L3: Query database (OFFLOADED to worker pool)
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
                        .subscribeOn(casbinScheduler)  // 🔥 CRITICAL FIX: Offload to worker pool
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
        })
        .subscribeOn(casbinScheduler);  // 🔥 Offload to worker pool
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
        })
        .subscribeOn(casbinScheduler);  // 🔥 Offload to worker pool
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
        })
        .subscribeOn(casbinScheduler);  // 🔥 Offload to worker pool
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
        })
        .subscribeOn(casbinScheduler);  // 🔥 Offload to worker pool
    }

    /**
     * Get all roles for a user in a tenant
     */
    public Mono<Set<String>> getUserRoles(String userId, String tenantId) {
        return Mono.fromCallable(() -> loadUserRolesFromDatabase(userId, tenantId))
            .subscribeOn(casbinScheduler);  // 🔥 Offload to worker pool
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
     * Invalidate all caches for a user (LOCAL + DISTRIBUTED)
     *
     * CRITICAL FIX: Broadcasts invalidation to ALL instances via Redis Pub/Sub
     *
     * Flow:
     * 1. Invalidate LOCAL L1 caches (Caffeine)
     * 2. Delete L2 Redis cache entries (best-effort)
     * 3. BROADCAST invalidation event to all IAM + Gateway instances
     * 4. All instances receive event and clear their L1 caches
     *
     * Result: Cache consistency across all instances within <10ms
     */
    private void invalidateUserCache(String userId, String tenantId) {
        String cacheKey = CacheConfig.UserPermissionContext.cacheKey(userId, tenantId);

        // Step 1: Clear LOCAL L1 cache (Caffeine)
        userPermissionsCache.invalidate(cacheKey);

        // Clear permission check cache (all entries for this user)
        permissionCheckCache.asMap().keySet().stream()
                .filter(key -> key.startsWith(String.format("user:%s:tenant:%s:", userId, tenantId)))
                .forEach(permissionCheckCache::invalidate);

        // Step 2: Delete L2 Redis cache (best-effort, fire-and-forget)
        redisTemplate.delete(cacheKey).subscribe(
            deleted -> log.trace("Deleted L2 Redis cache for user: {}", userId),
            error -> log.warn("Failed to delete L2 Redis cache for user: {}", userId, error)
        );

        // Delete permission check cache entries from Redis
        String permissionCachePattern = "permission:check:" + tenantId + ":" + userId + ":*";
        deleteRedisCachePattern(permissionCachePattern);

        // Step 3: BROADCAST invalidation to ALL instances (CRITICAL)
        cacheInvalidationService.invalidateUser(userId, tenantId, "permission_change")
            .doOnSuccess(v ->
                log.info("Broadcasted USER cache invalidation: userId={}, tenantId={}", userId, tenantId)
            )
            .doOnError(error ->
                log.error("Failed to broadcast USER cache invalidation: userId={}, tenantId={}, error={}",
                    userId, tenantId, error.getMessage())
            )
            .subscribe();  // Fire-and-forget (non-blocking)

        log.debug("Invalidated LOCAL cache for user {} in tenant {}", userId, tenantId);
    }

    /**
     * Invalidate policy caches for a tenant (LOCAL + DISTRIBUTED)
     *
     * CRITICAL FIX: Broadcasts invalidation to ALL instances
     *
     * Use case: Role permissions changed, affects all users with that role
     */
    private void invalidatePolicyCache(String tenantId) {
        // Step 1: Clear LOCAL L1 permission check cache for this tenant
        permissionCheckCache.asMap().keySet().stream()
                .filter(key -> key.contains(String.format(":tenant:%s:", tenantId)))
                .forEach(permissionCheckCache::invalidate);

        // Step 2: Delete L2 Redis cache entries (best-effort)
        String permissionCachePattern = "permission:check:" + tenantId + ":*";
        deleteRedisCachePattern(permissionCachePattern);

        // Step 3: BROADCAST tenant invalidation to ALL instances (CRITICAL)
        cacheInvalidationService.invalidateTenant(tenantId, "policy_change")
            .doOnSuccess(v ->
                log.info("Broadcasted TENANT cache invalidation: tenantId={}", tenantId)
            )
            .doOnError(error ->
                log.error("Failed to broadcast TENANT cache invalidation: tenantId={}, error={}",
                    tenantId, error.getMessage())
            )
            .subscribe();  // Fire-and-forget (non-blocking)

        log.debug("Invalidated LOCAL policy cache for tenant {}", tenantId);
    }

    /**
     * Delete Redis cache keys matching pattern using SCAN (non-blocking)
     *
     * Uses SCAN instead of KEYS to avoid blocking Redis
     * Fire-and-forget pattern for best-effort deletion
     *
     * @param pattern Redis key pattern (e.g., "permission:check:tenant-123:*")
     */
    private void deleteRedisCachePattern(String pattern) {
        redisTemplate.scan(org.springframework.data.redis.core.ScanOptions
                .scanOptions()
                .match(pattern)
                .count(100)  // Batch size
                .build())
            .flatMap(key -> redisTemplate.delete(key))
            .reduce(0L, Long::sum)
            .doOnSuccess(deletedCount ->
                log.trace("Deleted {} Redis cache entries matching pattern: {}", deletedCount, pattern)
            )
            .doOnError(error ->
                log.warn("Failed to delete Redis cache pattern {}: {}", pattern, error.getMessage())
            )
            .subscribe();  // Fire-and-forget (non-blocking)
    }
}
