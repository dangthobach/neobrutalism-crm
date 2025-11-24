'use client';

import { createContext, useContext, ReactNode } from 'react';
import { useWebSocketNotifications } from '@/hooks/useWebSocketNotifications';

interface WebSocketContextValue {
  isConnected: boolean;
  isReconnecting: boolean;
  reconnectAttempt: number;
  error: Error | null;
  unreadCount: number;
  reconnect: () => Promise<void>;
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

interface WebSocketProviderProps {
  children: ReactNode;
  userId?: string;
  enabled?: boolean;
}

/**
 * WebSocket Provider Component
 * Provides WebSocket connection context to the entire app
 * 
 * Usage:
 * ```tsx
 * <WebSocketProvider userId={user?.id}>
 *   {children}
 * </WebSocketProvider>
 * ```
 */
export function WebSocketProvider({ 
  children, 
  userId,
  enabled = true 
}: WebSocketProviderProps) {
  const websocket = useWebSocketNotifications({
    userId,
    enabled: enabled && !!userId,
    showToast: true,
  });

  return (
    <WebSocketContext.Provider value={websocket}>
      {children}
    </WebSocketContext.Provider>
  );
}

/**
 * Hook to use WebSocket context
 * 
 * Usage:
 * ```tsx
 * const { isConnected, unreadCount } = useWebSocket();
 * ```
 */
export function useWebSocket() {
  const context = useContext(WebSocketContext);
  
  if (!context) {
    throw new Error('useWebSocket must be used within WebSocketProvider');
  }
  
  return context;
}

/**
 * Optional: Hook to use WebSocket context without throwing error
 */
export function useWebSocketOptional() {
  return useContext(WebSocketContext);
}
