package com.neobrutalism.crm.gateway.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * IAM Service Client with Circuit Breaker
 *
 * Communicates with IAM Service for:
 * - User permission loading
 * - Permission validation
 *
 * Resilience Features:
 * - Circuit breaker (Resilience4j) - Fails fast when service is down
 * - Retry with exponential backoff - Handles transient failures
 * - Timeout handling - Prevents hanging requests
 * - Graceful degradation - Returns safe defaults on failure
 *
 * Circuit Breaker States:
 * - CLOSED: Normal operation, requests flow through
 * - OPEN: Service unhealthy, requests fail immediately
 * - HALF_OPEN: Testing if service recovered
 */
@Service
@Slf4j
public class IamServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    @Value("${app.gateway.iam-service.url:http://iam-service:8081}")
    private String iamServiceUrl;

    @Value("${app.gateway.iam-service.timeout:5000}")
    private long timeoutMs;

    public IamServiceClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.gateway.iam-service.url:http://iam-service:8081}") String iamServiceUrl
    ) {
        this.iamServiceUrl = iamServiceUrl;
        this.webClient = webClientBuilder
                .baseUrl(iamServiceUrl)
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("iam-service");

        // Log circuit breaker events
        this.circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("IAM Service circuit breaker state transition: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState())
                )
                .onError(event ->
                        log.error("IAM Service circuit breaker recorded error: {}",
                                event.getThrowable().getMessage())
                );
    }

    /**
     * Get user permissions from IAM service
     *
     * This endpoint in IAM service has L2 Redis cache,
     * so it's very fast (~1-5ms)
     *
     * Circuit breaker protects against:
     * - IAM service downtime
     * - Network issues
     * - Cascading failures
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Map of resource -> Set of actions
     */
    public Mono<Map<String, Set<String>>> getUserPermissions(String userId, String tenantId) {
        return webClient
                .get()
                .uri("/api/v1/auth/permissions/user/{userId}?tenantId={tenantId}",
                        userId, tenantId)
                .retrieve()
                .bodyToMono(PermissionsResponse.class)
                .map(PermissionsResponse::permissions)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofMillis(500))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                )
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(permissions ->
                        log.trace("Loaded {} permissions for user {} from IAM service",
                                permissions.size(), userId))
                .doOnError(error ->
                        log.error("Failed to load permissions for user {}: {}",
                                userId, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Returning empty permissions due to IAM service error: {}",
                            error.getMessage());
                    return Mono.just(Map.of());  // Return empty map on error (fail-safe)
                });
    }

    /**
     * Check if user has specific permission
     *
     * This is a fallback when permission is not in cache
     *
     * Circuit breaker ensures fail-fast behavior
     * Returns false (deny) on error for security
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param resource Resource (e.g., "/api/customers")
     * @param action Action (e.g., "GET")
     * @return true if allowed
     */
    public Mono<Boolean> checkPermission(String userId, String tenantId,
                                          String resource, String action) {
        return webClient
                .post()
                .uri("/api/v1/auth/check-permission")
                .bodyValue(new PermissionCheckRequest(userId, tenantId, resource, action))
                .retrieve()
                .bodyToMono(PermissionCheckResponse.class)
                .map(PermissionCheckResponse::allowed)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofMillis(500))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                )
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(allowed ->
                        log.trace("Permission check for user {}, resource {}, action {}: {}",
                                userId, resource, action, allowed ? "ALLOWED" : "DENIED"))
                .doOnError(error ->
                        log.error("Permission check failed for user {}: {}",
                                userId, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Denying access due to IAM service error: {}", error.getMessage());
                    return Mono.just(false);  // Deny on error (fail-secure)
                });
    }

    // DTOs
    private record PermissionsResponse(
            String userId,
            String tenantId,
            Map<String, Set<String>> permissions
    ) {}

    private record PermissionCheckRequest(
            String userId,
            String tenantId,
            String resource,
            String action
    ) {}

    private record PermissionCheckResponse(
            boolean allowed,
            String userId,
            String resource,
            String action
    ) {}
}
