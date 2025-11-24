'use client';

import { useEffect, useCallback, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { 
  WebSocketClient, 
  NotificationMessage, 
  UnreadCountMessage,
  initializeWebSocket 
} from '@/lib/websocket-client';
import { toast } from 'sonner';

interface UseWebSocketNotificationsOptions {
  userId?: string;
  enabled?: boolean;
  onNotification?: (notification: NotificationMessage) => void;
  onUnreadCountUpdate?: (count: number) => void;
  showToast?: boolean;
}

interface WebSocketState {
  isConnected: boolean;
  isReconnecting: boolean;
  reconnectAttempt: number;
  error: Error | null;
}

/**
 * React Hook for WebSocket real-time notifications
 * 
 * Features:
 * - Auto-connect/disconnect based on userId
 * - Real-time notification delivery
 * - Unread count updates
 * - Automatic React Query cache invalidation
 * - Toast notifications for new messages
 * - Reconnection handling
 * 
 * @example
 * ```tsx
 * const { isConnected, unreadCount } = useWebSocketNotifications({
 *   userId: user.id,
 *   showToast: true,
 * });
 * ```
 */
export function useWebSocketNotifications(options: UseWebSocketNotificationsOptions = {}) {
  const {
    userId,
    enabled = true,
    onNotification,
    onUnreadCountUpdate,
    showToast = true,
  } = options;

  const queryClient = useQueryClient();
  const wsClientRef = useRef<WebSocketClient | null>(null);
  
  const [state, setState] = useState<WebSocketState>({
    isConnected: false,
    isReconnecting: false,
    reconnectAttempt: 0,
    error: null,
  });

  const [unreadCount, setUnreadCount] = useState<number>(0);

  /**
   * Handle incoming notification
   */
  const handleNotification = useCallback((notification: NotificationMessage) => {
    console.log('[useWebSocketNotifications] Received notification:', notification);

    // Call custom handler
    onNotification?.(notification);

    // Show toast notification if enabled
    if (showToast) {
      const priorityColors = {
        0: 'default',
        1: 'default',
        2: 'warning',
        3: 'error',
      } as const;

      toast(notification.title, {
        description: notification.message,
        action: notification.actionUrl ? {
          label: 'View',
          onClick: () => {
            window.location.href = notification.actionUrl!;
          },
        } : undefined,
        duration: notification.priority >= 2 ? 10000 : 5000,
      });
    }

    // Invalidate notifications query to refetch
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
    queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });

  }, [onNotification, showToast, queryClient]);

  /**
   * Handle unread count update
   */
  const handleUnreadCountUpdate = useCallback((data: UnreadCountMessage) => {
    console.log('[useWebSocketNotifications] Unread count update:', data.unreadCount);
    
    setUnreadCount(data.unreadCount);
    onUnreadCountUpdate?.(data.unreadCount);

    // Update React Query cache
    queryClient.setQueryData(['notifications', 'unread-count'], data.unreadCount);

  }, [onUnreadCountUpdate, queryClient]);

  /**
   * Handle notification read event
   */
  const handleNotificationRead = useCallback((data: any) => {
    console.log('[useWebSocketNotifications] Notification read:', data.notificationId);

    // Invalidate queries to refetch updated data
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
    queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });

  }, [queryClient]);

  /**
   * Handle system messages
   */
  const handleSystemMessage = useCallback((data: any) => {
    console.log('[useWebSocketNotifications] System message:', data);

    if (showToast) {
      toast.info('System Message', {
        description: data.message,
      });
    }
  }, [showToast]);

  /**
   * Connect to WebSocket
   */
  const connect = useCallback(async () => {
    if (!userId || !enabled) return;

    try {
      // Initialize or get WebSocket client
      if (!wsClientRef.current) {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
        wsClientRef.current = initializeWebSocket(baseUrl);

        // Setup connection callbacks
        wsClientRef.current.onConnect(() => {
          setState(prev => ({ ...prev, isConnected: true, isReconnecting: false, error: null }));
          console.log('[useWebSocketNotifications] Connected');
        });

        wsClientRef.current.onDisconnect(() => {
          setState(prev => ({ ...prev, isConnected: false }));
          console.log('[useWebSocketNotifications] Disconnected');
        });

        wsClientRef.current.onError((error) => {
          setState(prev => ({ ...prev, error }));
          console.error('[useWebSocketNotifications] Error:', error);
        });

        wsClientRef.current.onReconnecting((attempt) => {
          setState(prev => ({ ...prev, isReconnecting: true, reconnectAttempt: attempt }));
          console.log('[useWebSocketNotifications] Reconnecting, attempt:', attempt);
        });
      }

      // Connect
      await wsClientRef.current.connect(userId);

      // Subscribe to notifications
      wsClientRef.current.subscribeToNotifications(userId, handleNotification);
      wsClientRef.current.subscribeToUnreadCount(userId, handleUnreadCountUpdate);
      wsClientRef.current.subscribeToNotificationRead(userId, handleNotificationRead);
      wsClientRef.current.subscribeToSystemMessages(userId, handleSystemMessage);

    } catch (error) {
      console.error('[useWebSocketNotifications] Failed to connect:', error);
      setState(prev => ({ ...prev, error: error as Error }));
    }
  }, [userId, enabled, handleNotification, handleUnreadCountUpdate, handleNotificationRead, handleSystemMessage]);

  /**
   * Disconnect from WebSocket
   */
  const disconnect = useCallback(async () => {
    if (wsClientRef.current) {
      await wsClientRef.current.disconnect();
      wsClientRef.current = null;
      setState({
        isConnected: false,
        isReconnecting: false,
        reconnectAttempt: 0,
        error: null,
      });
    }
  }, []);

  /**
   * Manual reconnect
   */
  const reconnect = useCallback(async () => {
    await disconnect();
    await connect();
  }, [connect, disconnect]);

  // Auto-connect/disconnect on mount/unmount and when userId changes
  useEffect(() => {
    if (userId && enabled) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [userId, enabled]); // Only reconnect when userId or enabled changes

  return {
    ...state,
    unreadCount,
    connect,
    disconnect,
    reconnect,
  };
}

/**
 * Simpler hook for just checking connection status
 */
export function useWebSocketConnection() {
  const [isConnected, setIsConnected] = useState(false);

  // This would integrate with the global WebSocket client
  // For now, return mock status
  
  return {
    isConnected,
  };
}
