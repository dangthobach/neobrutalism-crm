package com.neobrutalism.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

/**
 * Redis Session Configuration
 *
 * Purpose: Store user sessions in Redis for distributed deployments
 *
 * Features:
 * - Session stored in Redis (shared across Gateway instances)
 * - HttpOnly cookie for session ID (XSS protection)
 * - 30-minute session timeout
 * - Secure flag for HTTPS environments
 *
 * Benefits:
 * - Stateless Gateway instances (can scale horizontally)
 * - Session survives Gateway restarts
 * - Supports sticky session routing for performance
 */
@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes
public class SessionConfig {

    /**
     * Configure Session Cookie
     *
     * Security settings:
     * - HttpOnly: Prevents JavaScript access (XSS protection)
     * - Secure: Only sent over HTTPS (in production)
     * - SameSite: Strict (CSRF protection)
     */
    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();

        // Cookie name
        resolver.setCookieName("SESSION_ID");

        // Cookie settings
        resolver.addCookieInitializer(cookie -> {
            cookie.httpOnly(true);              // XSS protection
            cookie.secure(isProductionMode());   // HTTPS only in production
            cookie.sameSite("Strict");           // CSRF protection
            cookie.maxAge(java.time.Duration.ofMinutes(30));
            cookie.path("/");
        });

        return resolver;
    }

    /**
     * Check if running in production mode
     */
    private boolean isProductionMode() {
        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        return profile != null && (profile.contains("prod") || profile.contains("production"));
    }
}
