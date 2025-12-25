package com.neobrutalism.crm.config;

import com.neobrutalism.crm.common.ratelimit.RateLimitFilter;
import com.neobrutalism.crm.common.security.CustomUserDetailsService;
import com.neobrutalism.crm.common.security.JwtAuthenticationEntryPoint;
import com.neobrutalism.crm.common.security.JwtAuthenticationFilter;
import com.neobrutalism.crm.config.security.CasbinAuthorizationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration
 * Configures Spring Security with JWT authentication, rate limiting, and Casbin authorization
 * 
 * Authorization Strategy:
 * - Uses jCasbin for dynamic role-based authorization (no @PreAuthorize)
 * - Policies loaded from RoleMenu table on startup
 * - Multi-tenant support via domain model
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = false) // ⚠️ Disabled - using jCasbin instead
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserDetailsService;
    private final Environment environment;

    // Gateway authentication filter (trusts X-User-Id header)
    @Autowired(required = false)
    private com.neobrutalism.crm.common.security.GatewayAuthenticationFilter gatewayAuthenticationFilter;

    // Optional: Rate limiting filter (only available when rate-limit.enabled=true)
    @Autowired(required = false)
    private RateLimitFilter rateLimitFilter;

    // Optional: Casbin authorization filter (only available when casbin.enabled=true)
    @Autowired(required = false)
    private CasbinAuthorizationFilter casbinAuthorizationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                    auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // WebSocket endpoints (SockJS handshake needs to be public)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/app/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/queue/**").permitAll()

                        // Public GET endpoints
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll();

                    // Dev-only: Allow organizations API without authentication
                    if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                        auth.requestMatchers("/api/organizations/**").permitAll();
                    }

                    // All other requests require authentication
                    auth.anyRequest().authenticated();
                })
                .headers(headers -> {
                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self' https://eu.i.posthog.com https://eu-assets.i.posthog.com; frame-ancestors 'self'; form-action 'self'; base-uri 'self';")
                    );
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000)
                    );
                    headers.frameOptions(frame -> frame.sameOrigin());
                    headers.referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    headers.permissionsPolicy(policy -> policy
                            .policy("geolocation=(), microphone=(), camera=(), payment=(), usb=()")
                    );
                    headers.contentTypeOptions(org.springframework.security.config.Customizer.withDefaults());
                });

        // Add rate limiting filter if enabled (requires Redis)
        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }

        // ⭐ NEW: Gateway authentication filter (trusts X-User-Id header)
        // Priority: HIGHEST - runs before JWT filter
        // If X-User-Id header present (from Gateway), skip JWT validation
        if (gatewayAuthenticationFilter != null) {
            http.addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        // JWT authentication filter (backward compatibility)
        // Only runs if Gateway filter didn't authenticate
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Casbin authorization filter (dynamic role-based authorization) - only if enabled
        if (casbinAuthorizationFilter != null) {
            http.addFilterAfter(casbinAuthorizationFilter, JwtAuthenticationFilter.class);
        }

        // Allow H2 console iframe (already set via headers config)

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ SECURITY: Load CORS origins from environment variable for production flexibility
        String corsOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS",
            "http://localhost:3000,http://localhost:5173");
        List<String> allowedOrigins = Arrays.stream(corsOrigins.split(","))
            .map(String::trim)
            .toList();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // ✅ FIX: Remove wildcard, specify exact headers needed
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Tenant-ID",
            "X-Request-ID",
            "X-Organization-ID"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size"
        ));
        configuration.setAllowCredentials(true);
        // ✅ FIX: Reduce maxAge from 3600s to 600s (10 minutes)
        configuration.setMaxAge(600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Provider using CustomUserDetailsService
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
