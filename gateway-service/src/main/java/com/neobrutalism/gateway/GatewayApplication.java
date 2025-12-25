package com.neobrutalism.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * High-performance API Gateway Application
 * 
 * Features:
 * - Reactive, non-blocking architecture (100k+ CCU support)
 * - Service discovery and registration
 * - Rate limiting
 * - L1 (Caffeine) and L2 (Redis) caching
 * - Request coalescing
 * - Circuit breaking
 * - Load balancing
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

