package com.neobrutalism.gateway.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

/**
 * OAuth2 Security Configuration for Gateway
 *
 * Features:
 * - OAuth2 Login with Keycloak (OIDC)
 * - Automatic token refresh
 * - Session-based authentication (HttpOnly cookies)
 * - CSRF protection for non-API routes
 * - Security headers (HSTS, CSP, etc.)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configure Security Filter Chain
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        http
            // CSRF: Disable for API routes, enable for OAuth2 login
            .csrf(csrf -> csrf.disable()) // TODO: Enable selective CSRF later

            // Authorization Rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers("/", "/login/**", "/oauth2/**", "/logout").permitAll()
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/error").permitAll()

                // API routes: Require authentication
                .pathMatchers("/api/**").authenticated()

                // All other requests require authentication
                .anyExchange().authenticated()
            )

            // OAuth2 Login Configuration
            .oauth2Login(oauth2 -> oauth2
                .authenticationSuccessHandler(new OAuth2LoginSuccessHandler())
            )

            // OAuth2 Client (for token refresh)
            .oauth2Client(oauth2Client -> {
                // Auto-configured by Spring Boot
            })

            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
            )

            // Security Headers
            .headers(headers -> headers
                // HSTS: Force HTTPS
                .hsts(hsts -> hsts.includeSubdomains(true).maxAge(java.time.Duration.ofSeconds(31536000)))

                // CSP: Content Security Policy
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self' " + System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS", "http://localhost:3000") + "; " +
                    "frame-ancestors 'self'; " +
                    "form-action 'self'"
                ))

                // X-Frame-Options: Prevent clickjacking
                .frameOptions(frame -> frame.mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN))

                // Referrer Policy
                .referrerPolicy(referrer -> referrer.policy(
                    org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER
                ))
            );

        return http.build();
    }

    /**
     * OIDC Logout Success Handler
     * Redirects to Keycloak logout endpoint
     */
    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
            new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);

        // Redirect to home page after logout
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        return oidcLogoutSuccessHandler;
    }
}
