package com.neobrutalism.crm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 * Provides distributed caching for menu tree, permissions, and user/role lookups
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class RedisCacheConfig {

    /**
     * Object mapper for JSON serialization
     */
    @Bean
    public ObjectMapper redisCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Redis template for custom operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisCacheObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use Jackson serializer for values
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer =
            new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis cache manager with custom TTL per cache
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisCacheObjectMapper) {

        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper)))
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues();

        // Custom TTL per cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // ✅ PHASE 1 WEEK 2-3: Entity caches with optimized TTL
        // Short-lived caches (5 minutes) - Frequently changing data
        cacheConfigurations.put("branches", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("customers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("contacts", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Medium-lived caches (10 minutes) - User and group data
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("usergroups", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Long-lived caches (1 hour) - Rarely changing data
        cacheConfigurations.put("roles", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Menu tree - 1 hour (rarely changes)
        cacheConfigurations.put("menuTree", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User menus with permissions - 30 minutes
        cacheConfigurations.put("userMenus", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Role permissions - 1 hour
        cacheConfigurations.put("rolePermissions", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Permission matrix - 1 hour (rarely changes, invalidated on permission updates)
        cacheConfigurations.put("permissionMatrix", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User permissions - 30 minutes (invalidated on role assignment changes)
        cacheConfigurations.put("userPermissions", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // User by username/email - 15 minutes (for authentication)
        cacheConfigurations.put("userByUsername", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("userByEmail", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Role by code - 1 hour
        cacheConfigurations.put("roleByCode", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User roles - 30 minutes
        cacheConfigurations.put("userRoles", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Group members - 30 minutes
        cacheConfigurations.put("groupMembers", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // ✅ NOTIFICATION MODULE: High-scale caching for 1M users, 50K CCU
        // Notification preferences - 15 minutes (user checks frequently)
        cacheConfigurations.put("notification-preferences", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("notification-preference", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Notification list - 2 minutes (real-time updates via WebSocket)
        cacheConfigurations.put("notifications", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Unread count - 1 minute (displayed in UI header)
        cacheConfigurations.put("notification-unread-count", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Migration progress - 5 seconds TTL (real-time updates via WebSocket)
        cacheConfigurations.put("migration-progress", defaultConfig.entryTtl(Duration.ofSeconds(5)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}
