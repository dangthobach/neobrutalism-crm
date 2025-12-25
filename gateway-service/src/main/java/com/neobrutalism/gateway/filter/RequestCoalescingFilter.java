package com.neobrutalism.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Request coalescing filter
 * Groups identical requests within a time window to reduce backend load
 * Critical for handling 100k+ CCU efficiently
 */
@Slf4j
@Component
public class RequestCoalescingFilter extends AbstractGatewayFilterFactory<RequestCoalescingFilter.Config> {

    private final Map<String, CoalescingRequest> pendingRequests = new ConcurrentHashMap<>();

    @Value("${gateway.coalescing.enabled:true}")
    private boolean enabled;

    @Value("${gateway.coalescing.window-ms:100}")
    private long windowMs;

    @Value("${gateway.coalescing.max-wait-ms:500}")
    private long maxWaitMs;

    @Value("${gateway.coalescing.enabled-for-methods:GET,HEAD}")
    private Set<HttpMethod> enabledMethods;

    public RequestCoalescingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!enabled || !shouldCoalesce(exchange)) {
                return chain.filter(exchange);
            }

            String requestKey = generateRequestKey(exchange);
            CoalescingRequest coalescingRequest = pendingRequests.computeIfAbsent(
                    requestKey,
                    k -> new CoalescingRequest()
            );

            synchronized (coalescingRequest) {
                if (coalescingRequest.isInProgress()) {
                    // Another request is already in progress, wait for it
                    log.debug("Coalescing request: {}", requestKey);
                    return Mono.fromFuture(coalescingRequest.getFuture())
                            .flatMap(ex -> chain.filter(ex));
                } else {
                    // This is the first request, execute it
                    coalescingRequest.setInProgress(true);
                    CompletableFuture<ServerWebExchange> future = new CompletableFuture<>();
                    coalescingRequest.setFuture(future);

                    // Execute the request
                    return chain.filter(exchange)
                            .doOnSuccess(result -> {
                                // Complete all waiting requests
                                synchronized (coalescingRequest) {
                                    future.complete(exchange);
                                    pendingRequests.remove(requestKey);
                                }
                            })
                            .doOnError(error -> {
                                // Complete with error
                                synchronized (coalescingRequest) {
                                    future.completeExceptionally(error);
                                    pendingRequests.remove(requestKey);
                                }
                            });
                }
            }
        };
    }

    private boolean shouldCoalesce(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        return enabledMethods.contains(method);
    }

    private String generateRequestKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        String method = exchange.getRequest().getMethod().name();
        return method + ":" + path + (query != null ? "?" + query : "");
    }

    private static class CoalescingRequest {
        private volatile boolean inProgress;
        private CompletableFuture<ServerWebExchange> future;

        public boolean isInProgress() {
            return inProgress;
        }

        public void setInProgress(boolean inProgress) {
            this.inProgress = inProgress;
        }

        public CompletableFuture<ServerWebExchange> getFuture() {
            return future;
        }

        public void setFuture(CompletableFuture<ServerWebExchange> future) {
            this.future = future;
        }
    }

    public static class Config {
        // Configuration can be added here if needed
    }
}

