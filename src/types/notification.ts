/**
 * Notification Types
 * For real-time notification system (optimized for 1M users, 50K CCU)
 */

export enum NotificationType {
  SYSTEM = 'SYSTEM',
  TASK_ASSIGNED = 'TASK_ASSIGNED',
  TASK_UPDATED = 'TASK_UPDATED',
  TASK_COMPLETED = 'TASK_COMPLETED',
  TASK_COMMENT = 'TASK_COMMENT',
  CUSTOMER_CREATED = 'CUSTOMER_CREATED',
  CUSTOMER_UPDATED = 'CUSTOMER_UPDATED',
  ACTIVITY_REMINDER = 'ACTIVITY_REMINDER',
  ACTIVITY_OVERDUE = 'ACTIVITY_OVERDUE',
  MENTION = 'MENTION',
  APPROVAL_REQUEST = 'APPROVAL_REQUEST',
  APPROVAL_APPROVED = 'APPROVAL_APPROVED',
  APPROVAL_REJECTED = 'APPROVAL_REJECTED',
}

export enum NotificationPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

export enum NotificationStatus {
  UNREAD = 'UNREAD',
  READ = 'READ',
  ARCHIVED = 'ARCHIVED',
}

export interface Notification {
  id: string
  type: NotificationType
  priority: NotificationPriority
  title: string
  message: string
  entityType?: string
  entityId?: string
  actionUrl?: string
  metadata?: Record<string, any>
  isRead: boolean
  isArchived: boolean
  recipientId: string
  recipientName?: string
  senderId?: string
  senderName?: string
  senderAvatar?: string
  organizationId: string
  createdAt: string
  readAt?: string
  archivedAt?: string
}

export interface CreateNotificationRequest {
  type: NotificationType
  priority: NotificationPriority
  title: string
  message: string
  entityType?: string
  entityId?: string
  actionUrl?: string
  metadata?: Record<string, any>
  recipientIds: string[]
  organizationId: string
}

export interface NotificationSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  type?: NotificationType
  priority?: NotificationPriority
  isRead?: boolean
  isArchived?: boolean
  recipientId?: string
  createdAfter?: string
  createdBefore?: string
}

export interface NotificationStats {
  total: number
  unreadCount: number
  readCount: number
  archivedCount: number
  byType: Record<NotificationType, number>
  byPriority: Record<NotificationPriority, number>
  last24Hours: number
  last7Days: number
  last30Days: number
}

export interface NotificationPreferences {
  id: string
  userId: string
  emailEnabled: boolean
  pushEnabled: boolean
  inAppEnabled: boolean
  enabledTypes: NotificationType[]
  mutedUntil?: string
  createdAt: string
  updatedAt: string
}

export interface BatchMarkAsReadRequest {
  notificationIds: string[]
}

export interface WebSocketMessage {
  type: 'NOTIFICATION' | 'PING' | 'PONG' | 'CONNECTED' | 'DISCONNECTED'
  payload?: Notification
  timestamp: string
}
