package com.neobrutalism.gateway.config;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker configuration for resilience
 */
@Configuration
public class CircuitBreakerConfig {

    @Value("${gateway.circuit-breaker.enabled:true}")
    private boolean enabled;

    @Value("${gateway.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${gateway.circuit-breaker.wait-duration-in-open-state:10s}")
    private Duration waitDurationInOpenState;

    @Value("${gateway.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${gateway.circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(failureRateThreshold)
                        .waitDurationInOpenState(waitDurationInOpenState)
                        .slidingWindowSize(slidingWindowSize)
                        .minimumNumberOfCalls(minimumNumberOfCalls)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .build());
    }
}

