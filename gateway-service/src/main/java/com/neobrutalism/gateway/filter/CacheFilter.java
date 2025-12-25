package com.neobrutalism.gateway.filter;

import com.neobrutalism.gateway.cache.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Response caching filter with L1/L2 cache support
 * Caches GET and HEAD responses
 */
@Slf4j
@Component
public class CacheFilter extends AbstractGatewayFilterFactory<CacheFilter.Config> {

    private final CacheManager cacheManager;

    @Value("${gateway.cache.response-cache.enabled:true}")
    private boolean enabled;

    @Value("${gateway.cache.response-cache.ttl-seconds:60}")
    private int ttlSeconds;

    @Value("${gateway.cache.response-cache.cacheable-status-codes:200,304}")
    private List<Integer> cacheableStatusCodes;

    public CacheFilter(CacheManager cacheManager) {
        super(Config.class);
        this.cacheManager = cacheManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!enabled || !shouldCache(exchange)) {
                return chain.filter(exchange);
            }

            String cacheKey = generateCacheKey(exchange);
            
            // Try to get from cache
            return cacheManager.get(cacheKey)
                    .flatMap(cachedResponse -> {
                        log.debug("Cache hit for key: {}", cacheKey);
                        return writeCachedResponse(exchange, cachedResponse);
                    })
                    .switchIfEmpty(
                            // Cache miss, proceed with request and cache response
                            chain.filter(exchange).then(Mono.defer(() -> {
                                return cacheResponse(exchange, cacheKey);
                            }))
                    );
        };
    }

    private boolean shouldCache(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        return method == HttpMethod.GET || method == HttpMethod.HEAD;
    }

    private String generateCacheKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        String key = path + (query != null ? "?" + query : "");
        return "response:" + key;
    }

    private Mono<Void> writeCachedResponse(ServerWebExchange exchange, String cachedResponse) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add("X-Cache", "HIT");
        response.getHeaders().add("Content-Type", "application/json");
        
        DataBuffer buffer = response.bufferFactory().wrap(cachedResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> cacheResponse(ServerWebExchange exchange, String cacheKey) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        
        // Only cache successful responses
        if (!cacheableStatusCodes.contains(originalResponse.getStatusCode().value())) {
            return Mono.empty();
        }

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                return Flux.from(body)
                        .collectList()
                        .flatMap(dataBuffers -> {
                            // Combine all data buffers
                            byte[] combined = new byte[dataBuffers.stream()
                                    .mapToInt(DataBuffer::readableByteCount)
                                    .sum()];
                            
                            int offset = 0;
                            for (DataBuffer buffer : dataBuffers) {
                                int length = buffer.readableByteCount();
                                buffer.read(combined, offset, length);
                                offset += length;
                                DataBufferUtils.release(buffer);
                            }

                            String responseBody = new String(combined, StandardCharsets.UTF_8);
                            
                            // Cache the response
                            cacheManager.put(cacheKey, responseBody, Duration.ofSeconds(ttlSeconds))
                                    .subscribe(
                                            null,
                                            error -> log.error("Failed to cache response", error)
                                    );

                            originalResponse.getHeaders().add("X-Cache", "MISS");
                            
                            DataBuffer buffer = originalResponse.bufferFactory().wrap(combined);
                            return originalResponse.writeWith(Mono.just(buffer));
                        });
            }
        };

        exchange.getAttributes().put("CACHED_RESPONSE", decoratedResponse);
        return Mono.empty();
    }

    public static class Config {
        // Configuration can be added here if needed
    }
}

