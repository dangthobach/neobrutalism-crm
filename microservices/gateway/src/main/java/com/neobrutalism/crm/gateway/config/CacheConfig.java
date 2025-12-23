package com.neobrutalism.crm.gateway.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.gateway.cache.jwt-validation.max-size:100000}")
    private long jwtCacheMaxSize;

    @Value("${app.gateway.cache.jwt-validation.ttl:60}")
    private long jwtCacheTtl;

    @Value("${app.gateway.cache.permission-check.max-size:50000}")
    private long permissionCacheMaxSize;

    @Value("${app.gateway.cache.permission-check.ttl:60}")
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

        return Caffeine.newBuilder()
                .maximumSize(jwtCacheMaxSize)
                .expireAfterWrite(Duration.ofSeconds(jwtCacheTtl))
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.trace("JWT cache eviction: cause={}", cause))
                .build();
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

        return Caffeine.newBuilder()
                .maximumSize(permissionCacheMaxSize)
                .expireAfterWrite(Duration.ofSeconds(permissionCacheTtl))
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.trace("Permission cache eviction: key={}, cause={}", key, cause))
                .build();
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
