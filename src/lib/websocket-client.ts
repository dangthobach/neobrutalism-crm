import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketConfig {
  url: string;
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  debug?: boolean;
}

export interface NotificationMessage {
  id: string;
  title: string;
  message: string;
  type: string;
  priority: number;
  status: string;
  actionUrl?: string;
  entityType?: string;
  entityId?: string;
  isRead: boolean;
  createdAt: string;
  recipientId: string;
}

export interface UnreadCountMessage {
  type: 'UNREAD_COUNT_UPDATE';
  unreadCount: number;
  timestamp: number;
}

export interface NotificationReadMessage {
  type: 'NOTIFICATION_READ';
  notificationId: string;
  timestamp: number;
}

type MessageHandler = (message: any) => void;

/**
 * WebSocket Client for real-time notifications
 * Features:
 * - Auto-reconnection with exponential backoff
 * - Heartbeat for connection health
 * - Multiple subscription support
 * - Connection state management
 */
export class WebSocketClient {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private isConnecting = false;
  private config: Required<WebSocketConfig>;

  // Connection state callbacks
  private onConnectCallback?: () => void;
  private onDisconnectCallback?: () => void;
  private onErrorCallback?: (error: any) => void;
  private onReconnectingCallback?: (attempt: number) => void;

  constructor(config: WebSocketConfig) {
    this.config = {
      url: config.url,
      reconnectDelay: config.reconnectDelay || 5000,
      heartbeatIncoming: config.heartbeatIncoming || 10000,
      heartbeatOutgoing: config.heartbeatOutgoing || 10000,
      debug: config.debug || false,
    };
  }

  /**
   * Connect to WebSocket server
   */
  connect(userId?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        resolve();
        return;
      }

      if (this.isConnecting) {
        reject(new Error('Already connecting'));
        return;
      }

      this.isConnecting = true;

      try {
        // Create STOMP client with SockJS
        this.client = new Client({
          webSocketFactory: () => new SockJS(this.config.url) as any,
          
          // Heartbeat configuration
          heartbeatIncoming: this.config.heartbeatIncoming,
          heartbeatOutgoing: this.config.heartbeatOutgoing,
          
          // Reconnection
          reconnectDelay: this.config.reconnectDelay,
          
          // Debug logging
          debug: this.config.debug ? (msg: string) => console.log('[WebSocket]', msg) : undefined,

          // Connection callbacks
          onConnect: () => {
            console.log('[WebSocket] Connected successfully');
            this.isConnecting = false;
            this.reconnectAttempts = 0;
            this.onConnectCallback?.();
            resolve();
          },

          onDisconnect: () => {
            console.log('[WebSocket] Disconnected');
            this.isConnecting = false;
            this.onDisconnectCallback?.();
          },

          onStompError: (frame) => {
            console.error('[WebSocket] STOMP error:', frame);
            this.isConnecting = false;
            const error = new Error(frame.headers['message'] || 'STOMP error');
            this.onErrorCallback?.(error);
            reject(error);
          },

          onWebSocketError: (event) => {
            console.error('[WebSocket] WebSocket error:', event);
            this.isConnecting = false;
            this.onErrorCallback?.(event);
            reject(event);
          },

          onWebSocketClose: () => {
            console.log('[WebSocket] WebSocket connection closed');
            this.isConnecting = false;
            
            // Attempt reconnection with exponential backoff
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
              this.reconnectAttempts++;
              const delay = Math.min(
                this.config.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
                30000 // Max 30 seconds
              );
              
              console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
              this.onReconnectingCallback?.(this.reconnectAttempts);
              
              setTimeout(() => {
                this.connect(userId);
              }, delay);
            } else {
              console.error('[WebSocket] Max reconnection attempts reached');
            }
          },
        });

        // Activate the client
        this.client.activate();

      } catch (error) {
        console.error('[WebSocket] Failed to create client:', error);
        this.isConnecting = false;
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.client) {
        resolve();
        return;
      }

      // Unsubscribe from all subscriptions
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // Deactivate client
      this.client.deactivate().then(() => {
        console.log('[WebSocket] Disconnected successfully');
        this.client = null;
        resolve();
      });
    });
  }

  /**
   * Subscribe to notifications for current user
   */
  subscribeToNotifications(userId: string, handler: MessageHandler): string {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    const destination = `/user/queue/notifications`;
    const subscriptionId = `notifications-${userId}`;

    // Unsubscribe if already subscribed
    this.unsubscribe(subscriptionId);

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const notification: NotificationMessage = JSON.parse(message.body);
        handler(notification);
      } catch (error) {
        console.error('[WebSocket] Failed to parse notification:', error);
      }
    });

    this.subscriptions.set(subscriptionId, subscription);
    console.log(`[WebSocket] Subscribed to notifications for user ${userId}`);
    
    return subscriptionId;
  }

  /**
   * Subscribe to unread count updates
   */
  subscribeToUnreadCount(userId: string, handler: MessageHandler): string {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    const destination = `/user/queue/notifications/count`;
    const subscriptionId = `unread-count-${userId}`;

    this.unsubscribe(subscriptionId);

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data: UnreadCountMessage = JSON.parse(message.body);
        handler(data);
      } catch (error) {
        console.error('[WebSocket] Failed to parse unread count:', error);
      }
    });

    this.subscriptions.set(subscriptionId, subscription);
    console.log(`[WebSocket] Subscribed to unread count for user ${userId}`);
    
    return subscriptionId;
  }

  /**
   * Subscribe to notification read events
   */
  subscribeToNotificationRead(userId: string, handler: MessageHandler): string {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    const destination = `/user/queue/notifications/read`;
    const subscriptionId = `notification-read-${userId}`;

    this.unsubscribe(subscriptionId);

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data: NotificationReadMessage = JSON.parse(message.body);
        handler(data);
      } catch (error) {
        console.error('[WebSocket] Failed to parse notification read event:', error);
      }
    });

    this.subscriptions.set(subscriptionId, subscription);
    console.log(`[WebSocket] Subscribed to notification read events for user ${userId}`);
    
    return subscriptionId;
  }

  /**
   * Subscribe to system messages
   */
  subscribeToSystemMessages(userId: string, handler: MessageHandler): string {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    const destination = `/user/queue/system`;
    const subscriptionId = `system-${userId}`;

    this.unsubscribe(subscriptionId);

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        handler(data);
      } catch (error) {
        console.error('[WebSocket] Failed to parse system message:', error);
      }
    });

    this.subscriptions.set(subscriptionId, subscription);
    console.log(`[WebSocket] Subscribed to system messages for user ${userId}`);
    
    return subscriptionId;
  }

  /**
   * Unsubscribe from a subscription
   */
  unsubscribe(subscriptionId: string): void {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
      console.log(`[WebSocket] Unsubscribed from ${subscriptionId}`);
    }
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.client?.connected || false;
  }

  /**
   * Set connection callbacks
   */
  onConnect(callback: () => void): void {
    this.onConnectCallback = callback;
  }

  onDisconnect(callback: () => void): void {
    this.onDisconnectCallback = callback;
  }

  onError(callback: (error: any) => void): void {
    this.onErrorCallback = callback;
  }

  onReconnecting(callback: (attempt: number) => void): void {
    this.onReconnectingCallback = callback;
  }

  /**
   * Send a message (for future use)
   */
  send(destination: string, body: any, headers?: any): void {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
      headers,
    });
  }
}

// Singleton instance
let wsClient: WebSocketClient | null = null;

/**
 * Get or create WebSocket client instance
 */
export function getWebSocketClient(config?: WebSocketConfig): WebSocketClient {
  if (!wsClient && config) {
    wsClient = new WebSocketClient(config);
  }
  
  if (!wsClient) {
    throw new Error('WebSocket client not initialized. Provide config on first call.');
  }
  
  return wsClient;
}

/**
 * Initialize WebSocket client with default configuration
 */
export function initializeWebSocket(baseUrl: string = 'http://localhost:8080'): WebSocketClient {
  const config: WebSocketConfig = {
    url: `${baseUrl}/ws`,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: process.env.NODE_ENV === 'development',
  };

  return getWebSocketClient(config);
}
