package com.neobrutalism.crm.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration Validator
 *
 * Validates critical security configurations on application startup.
 * Prevents application from starting with insecure default values in production.
 *
 * Validates:
 * - JWT secret strength
 * - Database password presence
 * - Redis password in production
 * - Default admin password changed
 * - HSTS enabled in production
 * - CORS origins configured
 */
@Component
@Slf4j
public class SecurityConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment environment;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    public SecurityConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> securityWarnings = new ArrayList<>();
        List<String> securityErrors = new ArrayList<>();

        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        log.info("=".repeat(80));
        log.info("SECURITY CONFIGURATION VALIDATION");
        log.info("Active Profile: {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("=".repeat(80));

        // 1. Validate JWT Secret
        validateJwtSecret(securityWarnings, securityErrors, isProduction);

        // 2. Validate Database Password
        validateDatabasePassword(securityWarnings, securityErrors, isProduction);

        // 3. Validate Redis Password
        validateRedisPassword(securityWarnings, isProduction);

        // 4. Validate CORS Configuration
        validateCorsConfiguration(securityWarnings, isProduction);

        // 5. Validate HSTS Configuration
        validateHstsConfiguration(securityWarnings, isProduction);

        // 6. Check Rate Limiting
        validateRateLimiting(securityWarnings, isProduction);

        // 7. Check Default Admin Password
        validateDefaultAdminPassword(securityWarnings);

        // Print results
        log.info("-".repeat(80));

        if (securityErrors.isEmpty() && securityWarnings.isEmpty()) {
            log.info("✅ Security configuration validation passed! All checks OK.");
        } else {
            if (!securityWarnings.isEmpty()) {
                log.warn("⚠️ Security Warnings ({}):", securityWarnings.size());
                securityWarnings.forEach(warning -> log.warn("   - {}", warning));
            }

            if (!securityErrors.isEmpty()) {
                log.error("❌ Security Errors ({}):", securityErrors.size());
                securityErrors.forEach(error -> log.error("   - {}", error));

                if (isProduction) {
                    log.error("=".repeat(80));
                    log.error("CRITICAL: Application cannot start in PRODUCTION with security errors!");
                    log.error("=".repeat(80));
                    throw new IllegalStateException(
                        "Security validation failed in production. " +
                        "Fix the errors above before deploying to production."
                    );
                }
            }
        }

        log.info("=".repeat(80));
    }

    private void validateJwtSecret(List<String> warnings, List<String> errors, boolean isProduction) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            errors.add("JWT_SECRET is not configured!");
            return;
        }

        // Check for default/weak secrets
        String[] weakSecrets = {
            "neobrutalism-crm-secret-key-change-this-in-production-min-256-bits",
            "secret",
            "change-me",
            "changeme",
            "test-secret"
        };

        for (String weakSecret : weakSecrets) {
            if (jwtSecret.toLowerCase().contains(weakSecret.toLowerCase())) {
                if (isProduction) {
                    errors.add("JWT_SECRET contains default/weak value. Generate a strong secret with: openssl rand -base64 32");
                } else {
                    warnings.add("JWT_SECRET uses default value. This is OK for development but MUST be changed in production.");
                }
                return;
            }
        }

        // Check minimum length (256 bits = 32 bytes = 44 base64 chars)
        if (jwtSecret.length() < 32) {
            if (isProduction) {
                errors.add("JWT_SECRET is too short (< 32 characters). Generate a strong secret with: openssl rand -base64 32");
            } else {
                warnings.add("JWT_SECRET is shorter than recommended (32+ characters).");
            }
        } else {
            log.info("✅ JWT Secret: Strong ({} characters)", jwtSecret.length());
        }
    }

    private void validateDatabasePassword(List<String> warnings, List<String> errors, boolean isProduction) {
        if (dbPassword == null || dbPassword.isBlank()) {
            if (isProduction) {
                errors.add("DB_PASSWORD is not configured!");
            }
            return;
        }

        // Check for weak passwords
        String[] weakPasswords = {"postgres", "password", "123456", "admin", "changeme", "CHANGE_ME_DB_PASSWORD"};
        for (String weakPassword : weakPasswords) {
            if (dbPassword.toLowerCase().contains(weakPassword.toLowerCase())) {
                if (isProduction) {
                    errors.add("Database password contains weak/default value. Use a strong random password (32+ characters).");
                } else {
                    warnings.add("Database password uses default value. This is OK for development.");
                }
                return;
            }
        }

        if (dbPassword.length() < 16 && isProduction) {
            warnings.add("Database password is shorter than recommended (16+ characters).");
        } else {
            log.info("✅ Database Password: Configured");
        }
    }

    private void validateRedisPassword(List<String> warnings, boolean isProduction) {
        if (isProduction && (redisPassword == null || redisPassword.isBlank())) {
            warnings.add("Redis password not configured. Consider enabling Redis authentication in production.");
        } else if (redisPassword != null && !redisPassword.isBlank()) {
            log.info("✅ Redis Password: Configured");
        }
    }

    private void validateCorsConfiguration(List<String> warnings, boolean isProduction) {
        String corsOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS",
            "http://localhost:3000,http://localhost:5173");

        if (corsOrigins.contains("localhost") && isProduction) {
            warnings.add("CORS_ALLOWED_ORIGINS contains 'localhost'. Update to production URLs.");
        } else if (isProduction) {
            log.info("✅ CORS Origins: Configured for production");
        }
    }

    private void validateHstsConfiguration(List<String> warnings, boolean isProduction) {
        String hstsEnabled = environment.getProperty("SECURITY_HSTS_ENABLED", "false");

        if (isProduction && !"true".equals(hstsEnabled)) {
            warnings.add("HSTS is not enabled. Set SECURITY_HSTS_ENABLED=true in production.");
        } else if ("true".equals(hstsEnabled)) {
            log.info("✅ HSTS: Enabled");
        }
    }

    private void validateRateLimiting(List<String> warnings, boolean isProduction) {
        String rateLimitEnabled = environment.getProperty("rate-limit.enabled", "false");

        if (isProduction && !"true".equals(rateLimitEnabled)) {
            warnings.add("Rate limiting is not enabled. Set RATE_LIMIT_ENABLED=true in production.");
        } else if ("true".equals(rateLimitEnabled)) {
            log.info("✅ Rate Limiting: Enabled");
        }
    }

    private void validateDefaultAdminPassword(List<String> warnings) {
        // This is a reminder - actual password validation would require database access
        warnings.add("REMINDER: Ensure default admin password ('admin123') has been changed in production!");
    }
}
