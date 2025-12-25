package com.neobrutalism.crm.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Enhanced WebSocket configuration for real-time notifications
 * Optimized for 50K CCU with connection pooling and heartbeat
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Configure task scheduler for heartbeat
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        // Enable simple broker with heartbeat for connection health
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})  // Server and client heartbeat: 10 seconds
                .setTaskScheduler(taskScheduler);

        // Set application destination prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");

        log.info("Message broker configured with heartbeat: /topic, /queue, /app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000)  // SockJS heartbeat: 25 seconds
                .setDisconnectDelay(5000)  // Disconnect delay: 5 seconds
                .setStreamBytesLimit(512 * 1024)  // 512KB
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false);

        log.info("WebSocket endpoint registered: /ws with SockJS support");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure message size limits and buffer sizes for 50K CCU
        registration
                .setMessageSizeLimit(128 * 1024)  // 128KB per message
                .setSendBufferSizeLimit(512 * 1024)  // 512KB send buffer
                .setSendTimeLimit(20 * 1000)  // 20 seconds timeout
                .setTimeToFirstMessage(30 * 1000);  // 30 seconds for first message

        log.info("WebSocket transport configured with optimized limits for high concurrency");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Configure inbound channel executor for handling client messages
        registration.taskExecutor()
                .corePoolSize(20)
                .maxPoolSize(100)
                .queueCapacity(500)
                .keepAliveSeconds(60);

        log.info("Client inbound channel configured with thread pool");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Configure outbound channel executor for sending messages to clients
        registration.taskExecutor()
                .corePoolSize(20)
                .maxPoolSize(100)
                .queueCapacity(1000)
                .keepAliveSeconds(60);

        log.info("Client outbound channel configured with thread pool");
    }
}
