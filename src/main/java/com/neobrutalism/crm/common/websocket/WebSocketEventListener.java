package com.neobrutalism.crm.common.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket Event Listener for connection lifecycle management
 * Tracks connections for monitoring and diagnostics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final AtomicInteger connectedUsers = new AtomicInteger(0);
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * Handle client connection
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        sessionUserMap.put(sessionId, username);
        int count = connectedUsers.incrementAndGet();
        
        log.info("WebSocket connected - User: {}, Session: {}, Total connections: {}", 
                username, sessionId, count);
    }

    /**
     * Handle client disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = sessionUserMap.remove(sessionId);
        
        if (username != null) {
            int count = connectedUsers.decrementAndGet();
            log.info("WebSocket disconnected - User: {}, Session: {}, Total connections: {}", 
                    username, sessionId, count);
        }
    }

    /**
     * Handle subscription events
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.debug("WebSocket subscription - User: {}, Session: {}, Destination: {}", 
                username, sessionId, destination);
    }

    /**
     * Handle unsubscription events
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.debug("WebSocket unsubscription - User: {}, Session: {}", username, sessionId);
    }

    /**
     * Get current connected users count
     */
    public int getConnectedUsersCount() {
        return connectedUsers.get();
    }

    /**
     * Get session to user mapping
     */
    public Map<String, String> getSessionUserMap() {
        return new ConcurrentHashMap<>(sessionUserMap);
    }
}
