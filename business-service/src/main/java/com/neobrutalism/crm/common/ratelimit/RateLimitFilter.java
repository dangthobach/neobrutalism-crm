package com.neobrutalism.crm.common.ratelimit;

import com.neobrutalism.crm.common.security.JwtTokenProvider;
import com.neobrutalism.crm.common.security.UserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Rate limiting filter with role-based limits
 * - ADMIN: 1000 requests/minute
 * - USER: 100 requests/minute
 * - PUBLIC: 20 requests/minute
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${rate-limit.admin-limit:1000}")
    private int adminLimit;

    @Value("${rate-limit.user-limit:100}")
    private int userLimit;

    @Value("${rate-limit.public-limit:20}")
    private int publicLimit;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip rate limiting for actuator endpoints
        if (request.getRequestURI().startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        int limit = resolveLimit(request);

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofMinutes(1))
                        .build())
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);

        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            long remaining = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));

            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for key: {} (limit: {})", key, limit);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    /**
     * Resolve rate limit key based on user or IP
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            return "user:" + principal.getId();
        }

        // Extract JWT from request if available (before authentication)
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            try {
                if (jwtTokenProvider.validateToken(jwt)) {
                    String userId = jwtTokenProvider.getUserIdFromToken(jwt).toString();
                    return "user:" + userId;
                }
            } catch (Exception e) {
                log.debug("Failed to extract user from JWT: {}", e.getMessage());
            }
        }

        // Fall back to IP address for public/unauthenticated requests
        String ipAddress = getClientIP(request);
        return "ip:" + ipAddress;
    }

    /**
     * Resolve rate limit based on user role
     */
    private int resolveLimit(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Set<String> roles = principal.getRoles();

            // Check roles in priority order
            if (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN")) {
                return adminLimit;
            }
            return userLimit;
        }

        // Extract JWT from request if available (before authentication)
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            try {
                if (jwtTokenProvider.validateToken(jwt)) {
                    // User is authenticated, use user limit
                    return userLimit;
                }
            } catch (Exception e) {
                log.debug("Failed to validate JWT: {}", e.getMessage());
            }
        }

        // Public/unauthenticated requests get lowest limit
        return publicLimit;
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Get client IP address, considering proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }

        String xrHeader = request.getHeader("X-Real-IP");
        if (xrHeader != null && !xrHeader.isEmpty()) {
            return xrHeader;
        }

        return request.getRemoteAddr();
    }
}
