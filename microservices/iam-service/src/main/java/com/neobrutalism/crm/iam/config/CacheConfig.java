package com.neobrutalism.crm.iam.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Multi-tier Cache Configuration for IAM Service
 *
 * L1 Cache (Caffeine - In-Memory per instance):
 * - Permission checks: 50,000 entries, 60s TTL
 * - User roles: 10,000 entries, 60s TTL
 * - Hit rate target: >95%
 * - Latency: ~0.001ms
 *
 * L2 Cache (Redis - Distributed):
 * - Permissions: 300s TTL
 * - User sessions: 3600s TTL
 * - Latency: ~0.5ms
 *
 * Cache invalidation:
 * - On permission changes: Clear affected user caches
 * - On role changes: Clear affected users
 * - Redis pub/sub for cross-instance invalidation
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${permission.cache.l1.max-size:50000}")
    private long permissionCacheMaxSize;

    @Value("${permission.cache.l1.ttl-seconds:60}")
    private long permissionCacheTtl;

    @Value("${permission.cache.l2.ttl-seconds:300}")
    private long redisCacheTtl;

    /**
     * L1 Cache: Caffeine in-memory cache for permission checks
     * Key format: "user:{userId}:tenant:{tenantId}:resource:{resource}:action:{action}"
     * Value: Boolean (true = allowed, false = denied)
     */
    @Bean(name = "permissionCheckCache")
    public Cache<String, Boolean> permissionCheckCache() {
        return Caffeine.newBuilder()
                .maximumSize(permissionCacheMaxSize)
                .expireAfterWrite(Duration.ofSeconds(permissionCacheTtl))
                .recordStats()
                .build();
    }

    /**
     * L1 Cache: User permissions cache
     * Key format: "user:{userId}:tenant:{tenantId}"
     * Value: UserPermissionContext (roles + permissions map)
     */
    @Bean(name = "userPermissionsCache")
    public Cache<String, UserPermissionContext> userPermissionsCache() {
        return Caffeine.newBuilder()
                .maximumSize(permissionCacheMaxSize)
                .expireAfterWrite(Duration.ofSeconds(permissionCacheTtl))
                .recordStats()
                .build();
    }

    /**
     * L1 Cache: User roles cache
     * Key format: "user:{userId}:tenant:{tenantId}:roles"
     * Value: Set<String> (role names)
     */
    @Bean(name = "userRolesCache")
    public Cache<String, Set<String>> userRolesCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofSeconds(permissionCacheTtl))
                .recordStats()
                .build();
    }

    /**
     * L1 Cache Manager for Spring Cache annotations
     */
    @Bean(name = "caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "permissions", "roles", "users"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(permissionCacheMaxSize)
                .expireAfterWrite(Duration.ofSeconds(permissionCacheTtl))
                .recordStats());
        return cacheManager;
    }

    /**
     * L2 Cache Manager: Redis distributed cache
     */
    @Bean(name = "redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(redisCacheTtl))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        // Different TTL for different cache types
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "permissions", defaultConfig.entryTtl(Duration.ofSeconds(redisCacheTtl)),
                "roles", defaultConfig.entryTtl(Duration.ofSeconds(redisCacheTtl)),
                "user-sessions", defaultConfig.entryTtl(Duration.ofSeconds(3600))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * User Permission Context
     * Cached representation of user's permissions
     */
    public record UserPermissionContext(
            String userId,
            String tenantId,
            Set<String> roles,
            Map<String, Set<String>> permissions, // resource -> actions
            long cachedAt
    ) {
        public static String cacheKey(String userId, String tenantId) {
            return String.format("user:%s:tenant:%s", userId, tenantId);
        }

        public boolean isExpired(long ttlSeconds) {
            return System.currentTimeMillis() - cachedAt > ttlSeconds * 1000;
        }
    }

    /**
     * Generate cache key for permission check
     */
    public static String permissionCacheKey(String userId, String tenantId, String resource, String action) {
        return String.format("user:%s:tenant:%s:resource:%s:action:%s",
                userId, tenantId, resource, action);
    }

    /**
     * Generate cache key for user roles
     */
    public static String userRolesCacheKey(String userId, String tenantId) {
        return String.format("user:%s:tenant:%s:roles", userId, tenantId);
    }
}
