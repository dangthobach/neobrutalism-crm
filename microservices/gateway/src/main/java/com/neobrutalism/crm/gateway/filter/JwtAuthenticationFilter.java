package com.neobrutalism.crm.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.neobrutalism.crm.gateway.config.CacheConfig;
import com.neobrutalism.crm.gateway.service.IamServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JWT Authentication Filter with Multi-tier Caching
 *
 * Performance optimization strategy:
 * 1. L1 Cache (Caffeine): Check local cache first (~0.001ms)
 * 2. JWT Validation: Decode and validate JWT (~5ms)
 * 3. L2 Cache (Redis): Call IAM service if needed (~10ms)
 * 4. Database: Last resort via IAM service (~50ms)
 *
 * Target performance:
 * - Cache hit rate: >95%
 * - Average latency: <5ms
 * - P99 latency: <50ms
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final Cache<String, CacheConfig.UserContext> jwtValidationCache;
    private final ReactiveJwtDecoder jwtDecoder;
    private final IamServiceClient iamServiceClient;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify",
            "/actuator/health",
            "/actuator/info",
            "/fallback"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String token = extractToken(request);
        if (token == null) {
            log.debug("No JWT token found in request to {}", path);
            return unauthorized(exchange);
        }

        // Multi-tier caching strategy
        return validateTokenWithCache(token)
                .flatMap(userContext -> {
                    if (userContext.isExpired()) {
                        log.debug("Token expired for user: {}", userContext.userId());
                        jwtValidationCache.invalidate(token);
                        return unauthorized(exchange);
                    }

                    // Enrich request with user context
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userContext.userId())
                            .header("X-Tenant-Id", userContext.tenantId())
                            .header("X-User-Roles", String.join(",", userContext.roles()))
                            .build();

                    // Store user context in exchange attributes for downstream filters
                    exchange.getAttributes().put("userContext", userContext);

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(error -> {
                    log.error("JWT validation failed for path {}: {}", path, error.getMessage());
                    return unauthorized(exchange);
                });
    }

    /**
     * Validate token with multi-tier caching
     *
     * Step 1: Check L1 cache (Caffeine - in-memory)
     * Step 2: Decode JWT and validate signature
     * Step 3: Call IAM service if needed (with L2 Redis cache)
     */
    private Mono<CacheConfig.UserContext> validateTokenWithCache(String token) {
        // L1: Check local cache first
        CacheConfig.UserContext cached = jwtValidationCache.getIfPresent(token);
        if (cached != null) {
            log.trace("L1 cache HIT for token");
            return Mono.just(cached);
        }

        log.trace("L1 cache MISS, validating JWT");

        // L2: Decode and validate JWT
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // Extract basic claims from JWT
                    String userId = jwt.getSubject();
                    String tenantId = jwt.getClaimAsString("tenant_id");

                    @SuppressWarnings("unchecked")
                    List<String> roles = jwt.getClaimAsStringList("roles");

                    // For high-performance: Load full permissions from IAM service
                    // This call will hit Redis L2 cache in IAM service
                    return iamServiceClient.getUserPermissions(userId, tenantId)
                            .map(permissions -> {
                                CacheConfig.UserContext userContext = new CacheConfig.UserContext(
                                        userId,
                                        tenantId,
                                        roles != null ? Set.copyOf(roles) : Set.of(),
                                        permissions,
                                        jwt.getExpiresAt()
                                );

                                // Store in L1 cache for next request
                                jwtValidationCache.put(token, userContext);
                                log.trace("Token validated and cached for user: {}", userId);

                                return userContext;
                            });
                })
                .doOnError(error -> log.error("JWT validation failed: {}", error.getMessage()));
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
