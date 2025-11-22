/**
 * Notification React Query Hooks
 * Optimized for high-scale: 1M users, 50K CCU
 * 
 * Features:
 * - Smart polling with exponential backoff
 * - Stale-while-revalidate caching
 * - Optimistic updates
 * - Request deduplication
 * - Memory-efficient pagination
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationApi } from '@/lib/api/notifications'
import type {
  Notification,
  NotificationSearchParams,
  CreateNotificationRequest,
} from '@/types/notification'
import { toast } from 'sonner'
import { useEffect, useRef, useState } from 'react'

// Query keys
export const notificationKeys = {
  all: ['notifications'] as const,
  lists: () => [...notificationKeys.all, 'list'] as const,
  list: (params?: NotificationSearchParams) => [...notificationKeys.lists(), params] as const,
  details: () => [...notificationKeys.all, 'detail'] as const,
  detail: (id: string) => [...notificationKeys.details(), id] as const,
  unreadCount: () => [...notificationKeys.all, 'unread-count'] as const,
  recent: () => [...notificationKeys.all, 'recent'] as const,
  stats: () => [...notificationKeys.all, 'stats'] as const,
}

/**
 * Smart polling interval with exponential backoff
 * Reduces server load during low activity periods
 */
function useAdaptivePolling(hasUnread: boolean) {
  const [interval, setInterval] = useState(30000) // Start at 30s
  const consecutiveEmptyPolls = useRef(0)

  useEffect(() => {
    if (hasUnread) {
      // Reset to fast polling when there are unread notifications
      setInterval(30000)
      consecutiveEmptyPolls.current = 0
    } else {
      // Exponential backoff: 30s → 60s → 120s (max)
      consecutiveEmptyPolls.current++
      const newInterval = Math.min(30000 * Math.pow(2, consecutiveEmptyPolls.current), 120000)
      setInterval(newInterval)
    }
  }, [hasUnread])

  return interval
}

/**
 * Get paginated notifications
 */
export function useNotifications(params?: NotificationSearchParams) {
  return useQuery({
    queryKey: notificationKeys.list(params),
    queryFn: () => notificationApi.getNotifications(params),
    staleTime: 10000, // Consider data fresh for 10s
    gcTime: 300000, // Cache for 5 minutes
  })
}

/**
 * Get unread notification count with smart polling
 */
export function useUnreadCount() {
  const { data: count = 0 } = useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () => notificationApi.getUnreadCount(),
    staleTime: 10000, // 10s
    gcTime: 60000, // 1 min
  })

  const pollingInterval = useAdaptivePolling(count > 0)

  // Re-fetch with adaptive interval
  useQuery({
    queryKey: [...notificationKeys.unreadCount(), 'polling'],
    queryFn: () => notificationApi.getUnreadCount(),
    refetchInterval: pollingInterval,
    staleTime: pollingInterval,
  })

  return count
}

/**
 * Get recent notifications (for dropdown)
 */
export function useRecentNotifications() {
  return useQuery({
    queryKey: notificationKeys.recent(),
    queryFn: () => notificationApi.getRecentNotifications(),
    staleTime: 15000, // 15s
    gcTime: 60000, // 1 min
  })
}

/**
 * Get notification by ID
 */
export function useNotification(id: string | undefined) {
  return useQuery({
    queryKey: notificationKeys.detail(id!),
    queryFn: () => notificationApi.getNotificationById(id!),
    enabled: !!id,
  })
}

/**
 * Get notification statistics
 */
export function useNotificationStats() {
  return useQuery({
    queryKey: notificationKeys.stats(),
    queryFn: () => notificationApi.getNotificationStats(),
    staleTime: 30000, // 30s
  })
}

/**
 * Mark notification as read with optimistic update
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onMutate: async (id) => {
      // Optimistic update
      await queryClient.cancelQueries({ queryKey: notificationKeys.recent() })
      await queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount() })

      const previousRecent = queryClient.getQueryData<Notification[]>(notificationKeys.recent())
      const previousCount = queryClient.getQueryData<number>(notificationKeys.unreadCount())

      // Update cache optimistically
      if (previousRecent) {
        queryClient.setQueryData<Notification[]>(
          notificationKeys.recent(),
          previousRecent.map(n => n.id === id ? { ...n, isRead: true } : n)
        )
      }

      if (previousCount && previousCount > 0) {
        queryClient.setQueryData<number>(notificationKeys.unreadCount(), previousCount - 1)
      }

      return { previousRecent, previousCount }
    },
    onError: (error: any, id, context) => {
      // Rollback on error
      if (context?.previousRecent) {
        queryClient.setQueryData(notificationKeys.recent(), context.previousRecent)
      }
      if (context?.previousCount !== undefined) {
        queryClient.setQueryData(notificationKeys.unreadCount(), context.previousCount)
      }
      toast.error('Failed to mark as read')
    },
    onSuccess: () => {
      // Revalidate in background
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() })
      queryClient.invalidateQueries({ queryKey: notificationKeys.stats() })
    },
  })
}

/**
 * Mark multiple notifications as read (batch operation)
 */
export function useBatchMarkAsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: string[]) => notificationApi.batchMarkAsRead(ids),
    onSuccess: (_, ids) => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all })
      toast.success(`Marked ${ids.length} notification${ids.length > 1 ? 's' : ''} as read`)
    },
    onError: () => {
      toast.error('Failed to mark notifications as read')
    },
  })
}

/**
 * Mark all notifications as read
 */
export function useMarkAllAsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onMutate: async () => {
      // Optimistic update: set unread count to 0
      await queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount() })
      const previousCount = queryClient.getQueryData<number>(notificationKeys.unreadCount())
      queryClient.setQueryData<number>(notificationKeys.unreadCount(), 0)
      return { previousCount }
    },
    onError: (error: any, _, context) => {
      // Rollback
      if (context?.previousCount !== undefined) {
        queryClient.setQueryData(notificationKeys.unreadCount(), context.previousCount)
      }
      toast.error('Failed to mark all as read')
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all })
      toast.success('All notifications marked as read')
    },
  })
}

/**
 * Delete notification with optimistic update
 */
export function useDeleteNotification() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationApi.deleteNotification(id),
    onMutate: async (id) => {
      // Optimistic removal
      await queryClient.cancelQueries({ queryKey: notificationKeys.recent() })
      const previousRecent = queryClient.getQueryData<Notification[]>(notificationKeys.recent())

      if (previousRecent) {
        queryClient.setQueryData<Notification[]>(
          notificationKeys.recent(),
          previousRecent.filter(n => n.id !== id)
        )
      }

      return { previousRecent }
    },
    onError: (error: any, id, context) => {
      if (context?.previousRecent) {
        queryClient.setQueryData(notificationKeys.recent(), context.previousRecent)
      }
      toast.error('Failed to delete notification')
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all })
      toast.success('Notification deleted')
    },
  })
}

/**
 * Clear all read notifications
 */
export function useClearReadNotifications() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationApi.clearReadNotifications(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all })
      toast.success('Read notifications cleared')
    },
    onError: () => {
      toast.error('Failed to clear notifications')
    },
  })
}

/**
 * Create notification (admin only)
 */
export function useCreateNotification() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateNotificationRequest) => notificationApi.createNotification(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all })
      toast.success('Notification sent')
    },
    onError: () => {
      toast.error('Failed to send notification')
    },
  })
}

/**
 * Archive notification
 */
export function useArchiveNotification() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationApi.archiveNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all })
      toast.success('Notification archived')
    },
    onError: () => {
      toast.error('Failed to archive notification')
    },
  })
}
