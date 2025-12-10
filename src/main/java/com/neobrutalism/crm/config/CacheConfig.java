package com.neobrutalism.crm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine
 * Caches user sessions, roles, and permissions for performance
 * Only active when Redis cache is not configured
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
@ConditionalOnMissingBean(name = "cacheManager", type = "org.springframework.cache.CacheManager")
public class CacheConfig {

    public static final String USER_CACHE = "users";
    public static final String USER_ROLES_CACHE = "userRoles";
    public static final String USER_PERMISSIONS_CACHE = "userPermissions";
    public static final String USER_SESSION_CACHE = "userSessions";
    public static final String ROLES_CACHE = "roles";
    public static final String BRANCHES_CACHE = "branches";
    public static final String CUSTOMERS_CACHE = "customers";
    public static final String CONTACTS_CACHE = "contacts";
    public static final String GROUPS_CACHE = "usergroups";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                USER_CACHE,
                USER_ROLES_CACHE,
                USER_PERMISSIONS_CACHE,
                USER_SESSION_CACHE,
                ROLES_CACHE,
                BRANCHES_CACHE,
                CUSTOMERS_CACHE,
                CONTACTS_CACHE,
                GROUPS_CACHE
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
