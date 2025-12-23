package com.neobrutalism.crm.application.migration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time migration progress updates
 * Replaces SSE polling with efficient WebSocket broadcast
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class MigrationWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for broadcasting progress updates
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for broadcasting to subscribed clients
        config.enableSimpleBroker(
            "/topic/migration"  // Topic for migration progress updates
        );

        // Set application destination prefix for client messages
        config.setApplicationDestinationPrefixes("/app");

        log.info("WebSocket message broker configured for migration progress");
    }

    /**
     * Register WebSocket endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register endpoint for WebSocket connections
        registry.addEndpoint("/ws/migration")
                .setAllowedOriginPatterns("*")  // Configure properly for production
                .withSockJS();  // Fallback to SockJS for older browsers

        log.info("WebSocket endpoint registered: /ws/migration");
    }
}
