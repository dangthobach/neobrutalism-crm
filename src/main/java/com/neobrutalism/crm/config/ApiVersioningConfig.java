package com.neobrutalism.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * API Versioning Configuration
 *
 * Configures API versioning strategy and version-specific settings.
 *
 * Features:
 * - URL-based versioning (e.g., /api/v1, /api/v2)
 * - Version deprecation warnings
 * - Default version fallback
 * - Version-specific feature flags
 *
 * Configuration in application.yml:
 * <pre>
 * api:
 *   versioning:
 *     enabled: true
 *     default-version: v1
 *     deprecated-versions:
 *       - version: v0
 *         deprecation-date: 2024-12-01
 *         sunset-date: 2025-06-01
 *         migration-guide-url: https://docs.example.com/migration/v0-to-v1
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "api.versioning")
@Data
public class ApiVersioningConfig {

    /**
     * Enable/disable API versioning
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Default API version when version not specified
     * Default: v1
     */
    private String defaultVersion = "v1";

    /**
     * Current stable API version
     * Default: v1
     */
    private String currentVersion = "v1";

    /**
     * List of deprecated API versions
     */
    private Map<String, DeprecatedVersionInfo> deprecatedVersions = new HashMap<>();

    /**
     * Version-specific feature flags
     * Example: v2.new-authentication-flow = true
     */
    private Map<String, Boolean> featureFlags = new HashMap<>();

    /**
     * Deprecated version information
     */
    @Data
    public static class DeprecatedVersionInfo {
        /**
         * API version (e.g., "v0", "v1")
         */
        private String version;

        /**
         * Date when version was marked as deprecated (ISO 8601)
         */
        private String deprecationDate;

        /**
         * Date when version will be completely removed (ISO 8601)
         */
        private String sunsetDate;

        /**
         * URL to migration guide documentation
         */
        private String migrationGuideUrl;

        /**
         * Custom deprecation warning message
         */
        private String warningMessage;
    }

    /**
     * Check if a version is deprecated
     */
    public boolean isVersionDeprecated(String version) {
        return deprecatedVersions.containsKey(version);
    }

    /**
     * Get deprecation info for a version
     */
    public DeprecatedVersionInfo getDeprecationInfo(String version) {
        return deprecatedVersions.get(version);
    }

    /**
     * Check if a feature is enabled for a specific version
     */
    public boolean isFeatureEnabled(String version, String featureName) {
        String key = version + "." + featureName;
        return featureFlags.getOrDefault(key, false);
    }
}
