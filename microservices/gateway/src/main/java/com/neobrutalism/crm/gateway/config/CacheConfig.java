package com.neobrutalism.crm.gateway.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.neobrutalism.crm.gateway.service.AdaptiveTTLService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Multi-tier Caching Configuration
 *
 * L1 Cache (Caffeine - In-Memory):
 * - JWT Validation Cache: 100K entries, 60s TTL
 * - Permission Check Cache: 50K entries, 60s TTL
 *
 * Performance:
 * - L1 Hit: ~0.001ms (1M ops/sec)
 * - Target Hit Rate: >95%
 */
@Configuration
@Slf4j
public class CacheConfig {

    private final AdaptiveTTLService adaptiveTTLService;

    public CacheConfig(ObjectProvider<AdaptiveTTLService> adaptiveTTLServiceProvider) {
        this.adaptiveTTLService = adaptiveTTLServiceProvider.getIfAvailable();
    }

    @Value("${app.gateway.cache.jwt-validation.max-size:500000}")
    private long jwtCacheMaxSize;

    @Value("${app.gateway.cache.jwt-validation.ttl:300}")
    private long jwtCacheTtl;

    @Value("${app.gateway.cache.permission-check.max-size:200000}")
    private long permissionCacheMaxSize;

    @Value("${app.gateway.cache.permission-check.ttl:300}")
    private long permissionCacheTtl;

    /**
     * JWT Validation Cache
     *
     * Caches validated JWT tokens to avoid repeated validation
     * Key: JWT token string
     * Value: UserContext (userId, tenantId, roles, permissions)
     */
    @Bean
    public Cache<String, UserContext> jwtValidationCache() {
        log.info("Initializing JWT Validation Cache: maxSize={}, ttl={}s",
                jwtCacheMaxSize, jwtCacheTtl);

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(jwtCacheMaxSize)
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.trace("JWT cache eviction: cause={}", cause))
                .scheduler(com.github.benmanes.caffeine.cache.Scheduler.systemScheduler());

        if (adaptiveTTLService != null) {
            builder.expireAfter(
                    adaptiveExpiry(
                            AdaptiveTTLService.ResourceType.SESSION_DATA,
                            AdaptiveTTLService.FreshnessLevel.NORMAL
                    )
            );
        } else {
            builder.expireAfterWrite(Duration.ofSeconds(jwtCacheTtl));
        }

        return builder.<String, UserContext>build();
    }

    /**
     * Permission Check Cache
     *
     * Caches permission check results
     * Key: userId:tenantId:resource:action
     * Value: Boolean (allowed/denied)
     */
    @Bean
    public Cache<String, Boolean> permissionCheckCache() {
        log.info("Initializing Permission Check Cache: maxSize={}, ttl={}s",
                permissionCacheMaxSize, permissionCacheTtl);

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(permissionCacheMaxSize)
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.trace("Permission cache eviction: key={}, cause={}", key, cause))
                .scheduler(com.github.benmanes.caffeine.cache.Scheduler.systemScheduler());

        if (adaptiveTTLService != null) {
            builder.expireAfter(
                    adaptiveExpiry(
                            AdaptiveTTLService.ResourceType.USER_PERMISSIONS,
                            AdaptiveTTLService.FreshnessLevel.IMPORTANT
                    )
            );
        } else {
            builder.expireAfterWrite(Duration.ofSeconds(permissionCacheTtl));
        }

        return builder.<String, Boolean>build();
    }

    /**
     * Adaptive TTL expiry policy for Caffeine caches.
     *
     * Recomputes TTL on read/write so hot keys get extended TTL and cold keys shrink.
     */
    private <K, V> Expiry<K, V> adaptiveExpiry(
            AdaptiveTTLService.ResourceType resourceType,
            AdaptiveTTLService.FreshnessLevel freshnessLevel
    ) {
        return new Expiry<>() {
            @Override
            public long expireAfterCreate(K key, V value, long currentTime) {
                return ttlNanos(key);
            }

            @Override
            public long expireAfterUpdate(K key, V value, long currentTime, long currentDuration) {
                return ttlNanos(key);
            }

            @Override
            public long expireAfterRead(K key, V value, long currentTime, long currentDuration) {
                return ttlNanos(key);
            }

            private long ttlNanos(K key) {
                Duration ttl = adaptiveTTLService.calculateTTLInstant(
                        String.valueOf(key),
                        resourceType,
                        freshnessLevel
                );
                return ttl.toNanos();
            }
        };
    }

    /**
     * UserContext - Cached user information
     */
    public record UserContext(
            String userId,
            String tenantId,
            java.util.Set<String> roles,
            java.util.Map<String, java.util.Set<String>> permissions,
            java.time.Instant expiresAt
    ) {
        public boolean isExpired() {
            return expiresAt != null && expiresAt.isBefore(java.time.Instant.now());
        }

        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }

        public boolean hasPermission(String resource, String action) {
            if (permissions == null) return false;
            java.util.Set<String> actions = permissions.get(resource);
            return actions != null && actions.contains(action);
        }
    }
}
