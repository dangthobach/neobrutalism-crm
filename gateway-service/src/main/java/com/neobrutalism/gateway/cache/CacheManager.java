package com.neobrutalism.gateway.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Two-tier cache manager (L1: Caffeine, L2: Redis)
 * Provides ultra-fast response times with minimal roundtrips
 */
@Slf4j
@Component
public class CacheManager {

    private final Cache<String, String> l1Cache; // Caffeine (in-memory)
    private final ReactiveRedisTemplate<String, String> l2Cache; // Redis
    private final boolean l1Enabled;
    private final boolean l2Enabled;

    public CacheManager(
            @Value("${gateway.cache.l1.enabled:true}") boolean l1Enabled,
            @Value("${gateway.cache.l2.enabled:true}") boolean l2Enabled,
            @Value("${gateway.cache.l1.max-size:10000}") int l1MaxSize,
            @Value("${gateway.cache.l1.ttl-seconds:300}") int l1TtlSeconds,
            ReactiveRedisTemplate<String, String> redisTemplate) {
        this.l1Enabled = l1Enabled;
        this.l2Enabled = l2Enabled;
        this.l2Cache = redisTemplate;

        this.l1Cache = l1Enabled
                ? Caffeine.newBuilder()
                        .maximumSize(l1MaxSize)
                        .expireAfterWrite(Duration.ofSeconds(l1TtlSeconds))
                        .recordStats()
                        .build()
                : null;
    }

    /**
     * Get value from cache (L1 -> L2 -> null)
     */
    public Mono<String> get(String key) {
        // Try L1 cache first (synchronous, fastest)
        if (l1Enabled && l1Cache != null) {
            String value = l1Cache.getIfPresent(key);
            if (value != null) {
                log.debug("Cache hit L1: {}", key);
                return Mono.just(value);
            }
        }

        // Try L2 cache (Redis, asynchronous)
        if (l2Enabled && l2Cache != null) {
            return l2Cache.opsForValue().get(key)
                    .doOnNext(value -> {
                        if (value != null && l1Enabled && l1Cache != null) {
                            // Populate L1 cache
                            l1Cache.put(key, value);
                            log.debug("Cache hit L2, populated L1: {}", key);
                        }
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.debug("Cache miss: {}", key);
                        return Mono.empty();
                    }));
        }

        return Mono.empty();
    }

    /**
     * Put value into both L1 and L2 cache
     */
    public Mono<Void> put(String key, String value, Duration ttl) {
        CompletableFuture<Void> l1Future = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> l2Future = CompletableFuture.completedFuture(null);

        // Put in L1 cache
        if (l1Enabled && l1Cache != null) {
            l1Cache.put(key, value);
            log.debug("Cached in L1: {}", key);
        }

        // Put in L2 cache
        if (l2Enabled && l2Cache != null) {
            l2Future = l2Cache.opsForValue().set(key, value, ttl)
                    .then()
                    .doOnSuccess(v -> log.debug("Cached in L2: {}", key))
                    .toFuture();
        }

        return Mono.fromFuture(CompletableFuture.allOf(l1Future, l2Future));
    }

    /**
     * Invalidate cache (both L1 and L2)
     */
    public Mono<Void> evict(String key) {
        if (l1Enabled && l1Cache != null) {
            l1Cache.invalidate(key);
        }

        if (l2Enabled && l2Cache != null) {
            return l2Cache.delete(key).then();
        }

        return Mono.empty();
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return new CacheStats(
                l1Enabled && l1Cache != null ? l1Cache.stats() : null
        );
    }

    public static class CacheStats {
        private final com.github.benmanes.caffeine.cache.stats.CacheStats l1Stats;

        public CacheStats(com.github.benmanes.caffeine.cache.stats.CacheStats l1Stats) {
            this.l1Stats = l1Stats;
        }

        public long getL1HitCount() {
            return l1Stats != null ? l1Stats.hitCount() : 0;
        }

        public long getL1MissCount() {
            return l1Stats != null ? l1Stats.missCount() : 0;
        }

        public double getL1HitRate() {
            return l1Stats != null ? l1Stats.hitRate() : 0.0;
        }
    }
}

