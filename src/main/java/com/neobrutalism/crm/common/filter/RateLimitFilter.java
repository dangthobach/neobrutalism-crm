package com.neobrutalism.crm.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Filter
 * Applies rate limits to authentication and CRUD endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Determine which rate limit to apply
        String bucketKey;
        Bucket bucket;

        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            // Auth endpoints: rate limit by IP
            bucketKey = "auth:" + getClientIP(request);
            bucket = rateLimitConfig.resolveBucket(proxyManager, bucketKey, rateLimitConfig.authRateLimitConfig());
            log.debug("Applying auth rate limit for IP: {}", getClientIP(request));
        } else if (isWriteOperation(method)) {
            // CRUD write operations: rate limit by user
            bucketKey = "crud:" + getCurrentUser();
            bucket = rateLimitConfig.resolveBucket(proxyManager, bucketKey, rateLimitConfig.crudRateLimitConfig());
            log.debug("Applying CRUD rate limit for user: {}", getCurrentUser());
        } else if (isReadOperation(method)) {
            // Read operations: higher limit
            bucketKey = "read:" + getCurrentUser();
            bucket = rateLimitConfig.resolveBucket(proxyManager, bucketKey, rateLimitConfig.readRateLimitConfig());
            log.debug("Applying read rate limit for user: {}", getCurrentUser());
        } else {
            // No rate limit for other operations
            filterChain.doFilter(request, response);
            return;
        }

        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

            log.warn("Rate limit exceeded for key: {}. Wait {} seconds", bucketKey, waitForRefill);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));

            ApiResponse<Object> apiResponse = ApiResponse.error(
                "Too many requests. Please try again in " + waitForRefill + " seconds.",
                "RATE_LIMIT_EXCEEDED"
            );

            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }
    }

    private boolean isWriteOperation(String method) {
        return "POST".equals(method) || "PUT".equals(method) ||
               "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean isReadOperation(String method) {
        return "GET".equals(method);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
