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

        // Menu tree - 1 hour (rarely changes)
        cacheConfigurations.put("menuTree", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User menus with permissions - 30 minutes
        cacheConfigurations.put("userMenus", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Role permissions - 1 hour
        cacheConfigurations.put("rolePermissions", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User by username/email - 15 minutes (for authentication)
        cacheConfigurations.put("userByUsername", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("userByEmail", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Role by code - 1 hour
        cacheConfigurations.put("roleByCode", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User roles - 30 minutes
        cacheConfigurations.put("userRoles", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Group members - 30 minutes
        cacheConfigurations.put("groupMembers", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}
