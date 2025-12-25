package com.neobrutalism.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Rate limiting configuration using Spring Cloud Gateway's built-in Redis rate limiter
 * This is more efficient for reactive gateways than custom Bucket4j implementation
 */
@Slf4j
@Configuration
public class RateLimitFilter {

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${gateway.rate-limit.default-limit.replenish-rate:100}")
    private int defaultReplenishRate;

    @Value("${gateway.rate-limit.default-limit.burst-capacity:200}")
    private int defaultBurstCapacity;

    /**
     * Redis Rate Limiter Bean
     * Configured via application.yml route filters
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity);
    }

    /**
     * Key Resolver for rate limiting
     * Uses IP address or user ID from header
     */
    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> {
            // Try to get user ID from header first
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }

            // Fallback to IP address
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + clientIp);
        };
    }
}

