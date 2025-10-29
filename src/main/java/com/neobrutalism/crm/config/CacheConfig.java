package com.neobrutalism.crm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine
 * Caches user sessions, roles, and permissions for performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_CACHE = "users";
    public static final String USER_ROLES_CACHE = "userRoles";
    public static final String USER_PERMISSIONS_CACHE = "userPermissions";
    public static final String USER_SESSION_CACHE = "userSessions";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                USER_CACHE,
                USER_ROLES_CACHE,
                USER_PERMISSIONS_CACHE,
                USER_SESSION_CACHE
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache configuration
     * - Maximum 10,000 entries
     * - Expire after 30 minutes of inactivity
     * - Expire after 2 hours since creation
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats();
    }
}
