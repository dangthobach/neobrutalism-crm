package com.neobrutalism.crm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application
 *
 * High-performance reactive gateway for 100K CCU
 *
 * Features:
 * - JWT validation with multi-tier caching
 * - Permission-based authorization
 * - Rate limiting (Redis-backed)
 * - Circuit breaker pattern
 * - Request/Response transformation
 * - Distributed tracing
 * - Dynamic routing with canary deployment support
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
