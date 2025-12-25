package com.neobrutalism.crm.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Rate Limiting Configuration using Bucket4j
 * Provides distributed rate limiting with Redis backend
 */
@Configuration
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Redis client for rate limiting (separate from cache)
     */
    @Bean
    public RedisClient redisClientForRateLimit() {
        String uri = (redisPassword == null || redisPassword.isBlank())
                ? String.format("redis://%s:%d", redisHost, redisPort)
                : String.format("redis://:%s@%s:%d", urlEncode(redisPassword), redisHost, redisPort);
        return RedisClient.create(uri);
    }

    private String urlEncode(String raw) {
        try {
            return java.net.URLEncoder.encode(raw, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return raw;
        }
    }

    /**
     * Proxy manager for distributed rate limiting
     */
    @Bean
    public ProxyManager<String> proxyManager(RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
            RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
        return LettuceBasedProxyManager.builderFor(connection)
            .build();
    }

    /**
     * Rate limit configuration for authentication endpoints
     * 5 attempts per minute per IP
     */
    public Supplier<BucketConfiguration> authRateLimitConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Rate limit configuration for CRUD operations
     * 100 requests per minute per user
     */
    public Supplier<BucketConfiguration> crudRateLimitConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Rate limit configuration for read operations
     * 300 requests per minute per user
     */
    public Supplier<BucketConfiguration> readRateLimitConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Get or create bucket for rate limiting
     */
    public Bucket resolveBucket(ProxyManager<String> proxyManager, String key, Supplier<BucketConfiguration> configSupplier) {
        return proxyManager.builder().build(key, configSupplier);
    }
}
