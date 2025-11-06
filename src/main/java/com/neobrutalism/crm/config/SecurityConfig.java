package com.neobrutalism.crm.config;

import com.neobrutalism.crm.common.filter.RateLimitFilter;
import com.neobrutalism.crm.common.security.CustomUserDetailsService;
import com.neobrutalism.crm.common.security.JwtAuthenticationEntryPoint;
import com.neobrutalism.crm.common.security.JwtAuthenticationFilter;
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
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration
 * Configures Spring Security with JWT authentication, rate limiting, and Casbin authorization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserDetailsService;

    // Optional: Rate limiting filter (only available when rate-limit.enabled=true)
    @Autowired(required = false)
    private RateLimitFilter rateLimitFilter;

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
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Public GET endpoints
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
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

        // JWT authentication filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console iframe (already set via headers config)

        return http.build();
    }

    /**
     * Development-only security overrides
     * ⚠️ Only active in 'dev' profile - NEVER in production!
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityOverrides(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            // Allow organizations API in dev only
            .requestMatchers("/api/organizations/**").permitAll()
        );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
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
