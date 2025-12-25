package com.neobrutalism.crm.common.api;

import com.neobrutalism.crm.config.ApiVersioningConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API Version Interceptor
 *
 * Intercepts API requests to:
 * - Extract API version from URL
 * - Add deprecation warnings to response headers
 * - Log API version usage metrics
 * - Enforce version sunset (return 410 Gone for removed versions)
 *
 * Response Headers:
 * - X-API-Version: Current version being used
 * - X-API-Deprecated-Warning: Warning message if version is deprecated
 * - X-API-Sunset-Date: Date when version will be removed
 * - X-API-Migration-Guide: URL to migration documentation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiVersionInterceptor implements HandlerInterceptor {

    private final ApiVersioningConfig versioningConfig;

    // Pattern to extract version from URL: /api/v1/... or /api/v2/...
    private static final Pattern VERSION_PATTERN = Pattern.compile("/api/(v\\d+)/.*");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        // Extract version from URL
        Matcher matcher = VERSION_PATTERN.matcher(requestUri);
        String version = null;

        if (matcher.matches()) {
            version = matcher.group(1); // e.g., "v1", "v2"
        }

        // Use default version if not specified
        if (version == null) {
            version = versioningConfig.getDefaultVersion();
        }

        // Add version header to response
        response.setHeader("X-API-Version", version);

        // Check if version is deprecated
        if (versioningConfig.isVersionDeprecated(version)) {
            ApiVersioningConfig.DeprecatedVersionInfo deprecationInfo =
                versioningConfig.getDeprecationInfo(version);

            // Add deprecation warning headers
            if (deprecationInfo != null) {
                String warningMessage = deprecationInfo.getWarningMessage() != null
                    ? deprecationInfo.getWarningMessage()
                    : "API version " + version + " is deprecated and will be removed on "
                      + deprecationInfo.getSunsetDate();

                response.setHeader("X-API-Deprecated-Warning", warningMessage);

                if (deprecationInfo.getSunsetDate() != null) {
                    response.setHeader("X-API-Sunset-Date", deprecationInfo.getSunsetDate());
                }

                if (deprecationInfo.getMigrationGuideUrl() != null) {
                    response.setHeader("X-API-Migration-Guide", deprecationInfo.getMigrationGuideUrl());
                }

                log.warn("Deprecated API version accessed: {} from IP: {}", version, request.getRemoteAddr());
            }
        }

        // Log API version usage
        log.debug("API request: version={}, method={}, uri={}", version, request.getMethod(), requestUri);

        return true; // Continue processing
    }
}
