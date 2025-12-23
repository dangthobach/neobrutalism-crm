package com.neobrutalism.crm.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IAM Service Application
 *
 * Handles authentication and authorization for the CRM system:
 * - Authentication: Proxies to Keycloak for user authentication
 * - Authorization: Uses jCasbin for policy-based access control
 * - Caching: Multi-tier caching (L1 Caffeine + L2 Redis) for high performance
 *
 * Performance targets:
 * - Support 100K CCU
 * - Permission check < 5ms (with cache hit)
 * - Cache hit rate > 95%
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
