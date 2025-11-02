package com.neobrutalism.crm.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Security Headers Configuration
 * Adds security headers to all HTTP responses
 */
@Configuration
public class SecurityHeadersConfig {

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    public static class SecurityHeadersFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Content Security Policy
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self' ws: wss:; " +
                    "frame-ancestors 'none'");

            // Prevent clickjacking
            httpResponse.setHeader("X-Frame-Options", "DENY");

            // Prevent MIME type sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // XSS Protection (legacy, but still useful)
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

            // Force HTTPS (only in production)
            // Uncomment for production:
            // httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

            // Referrer Policy
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy (formerly Feature Policy)
            httpResponse.setHeader("Permissions-Policy",
                    "geolocation=(), " +
                    "microphone=(), " +
                    "camera=(), " +
                    "payment=(), " +
                    "usb=(), " +
                    "magnetometer=(), " +
                    "gyroscope=(), " +
                    "accelerometer=()");

            // Cache Control for sensitive data
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");

            chain.doFilter(request, response);
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // Initialization logic if needed
        }

        @Override
        public void destroy() {
            // Cleanup logic if needed
        }
    }
}
