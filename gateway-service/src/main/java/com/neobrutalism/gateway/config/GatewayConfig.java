package com.neobrutalism.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("business-service", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                        )
                        .uri("lb://business-service"))
                .build();
    }
}

