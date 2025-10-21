package com.neobrutalism.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing configuration
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * Auditor provider implementation
     * Override to get current user from security context
     */
    public static class AuditorAwareImpl implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            // TODO: Integrate with Spring Security to get current user
            // Example:
            // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // if (authentication == null || !authentication.isAuthenticated()) {
            //     return Optional.of("system");
            // }
            // return Optional.of(authentication.getName());

            return Optional.of("system");
        }
    }
}
