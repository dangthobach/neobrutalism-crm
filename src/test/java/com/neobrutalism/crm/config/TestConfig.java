package com.neobrutalism.crm.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to provide mock beans for testing
 * This prevents ApplicationContext failures when services are not available
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock JavaMailSender for tests (prevents mail server connection issues)
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
