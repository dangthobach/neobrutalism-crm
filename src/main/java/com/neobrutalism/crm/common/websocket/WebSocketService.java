package com.neobrutalism.crm.common.websocket;

import com.neobrutalism.crm.domain.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket Service for real-time notification delivery
 * Optimized for 50K CCU with efficient message routing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    /**
     * Send notification to specific user via WebSocket
     */
    public void sendNotificationToUser(UUID userId, Notification notification) {
        try {
            String destination = "/queue/notifications";
            
            // Convert notification to DTO to avoid lazy loading issues
            Map<String, Object> payload = buildNotificationPayload(notification);
            
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    payload
            );
            
            log.debug("Notification sent via WebSocket to user: {} - {}", userId, notification.getId());
            
        } catch (Exception e) {
            log.error("Failed to send notification via WebSocket to user: {}", userId, e);
        }
    }

    /**
     * Send notification to multiple users
     */
    public void sendNotificationToUsers(Iterable<UUID> userIds, Notification notification) {
        Map<String, Object> payload = buildNotificationPayload(notification);
        
        for (UUID userId : userIds) {
            try {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        payload
                );
            } catch (Exception e) {
                log.error("Failed to send notification via WebSocket to user: {}", userId, e);
            }
        }
        
        log.info("Bulk notification sent via WebSocket to multiple users");
    }

    /**
     * Broadcast notification to all connected users
     */
    public void broadcastNotification(Notification notification) {
        try {
            Map<String, Object> payload = buildNotificationPayload(notification);
            
            messagingTemplate.convertAndSend("/topic/notifications", payload);
            
            log.info("Notification broadcast to all users: {}", notification.getId());
            
        } catch (Exception e) {
            log.error("Failed to broadcast notification", e);
        }
    }

    /**
     * Send unread count update to user
     */
    public void sendUnreadCountUpdate(UUID userId, Long unreadCount) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "UNREAD_COUNT_UPDATE");
            payload.put("unreadCount", unreadCount);
            payload.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications/count",
                    payload
            );
            
            log.debug("Unread count update sent to user: {} - count: {}", userId, unreadCount);
            
        } catch (Exception e) {
            log.error("Failed to send unread count update to user: {}", userId, e);
        }
    }

    /**
     * Send notification marked as read event
     */
    public void sendNotificationReadEvent(UUID userId, UUID notificationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "NOTIFICATION_READ");
            payload.put("notificationId", notificationId.toString());
            payload.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications/read",
                    payload
            );
            
            log.debug("Notification read event sent to user: {} - notification: {}", userId, notificationId);
            
        } catch (Exception e) {
            log.error("Failed to send notification read event to user: {}", userId, e);
        }
    }

    /**
     * Check if user is currently connected
     */
    public boolean isUserConnected(UUID userId) {
        return userRegistry.getUser(userId.toString()) != null;
    }

    /**
     * Get count of connected users
     */
    public int getConnectedUsersCount() {
        return userRegistry.getUserCount();
    }

    /**
     * Build notification payload to send via WebSocket
     * Converts entity to simple map to avoid serialization issues
     */
    private Map<String, Object> buildNotificationPayload(Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId().toString());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("type", notification.getNotificationType().name());
        payload.put("priority", notification.getPriority());
        payload.put("status", notification.getStatus().name());
        payload.put("actionUrl", notification.getActionUrl());
        payload.put("entityType", notification.getEntityType());
        payload.put("entityId", notification.getEntityId() != null ? notification.getEntityId().toString() : null);
        payload.put("isRead", notification.getIsRead());
        payload.put("createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
        payload.put("recipientId", notification.getRecipientId().toString());
        
        return payload;
    }

    /**
     * Send system message to user
     */
    public void sendSystemMessage(UUID userId, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SYSTEM_MESSAGE");
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/system",
                    payload
            );
            
            log.debug("System message sent to user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to send system message to user: {}", userId, e);
        }
    }

    /**
     * Send heartbeat/ping to user to check connection
     */
    public void sendPing(UUID userId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PING");
            payload.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/ping",
                    payload
            );
            
        } catch (Exception e) {
            log.error("Failed to send ping to user: {}", userId, e);
        }
    }
}
