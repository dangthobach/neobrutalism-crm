'use client'

import React, { useMemo } from 'react'
import { NotificationItem } from './notification-item'
import { Card } from '@/components/ui/card'
import { Bell, Inbox } from 'lucide-react'
import type { Notification } from '@/types/notification'

interface NotificationListProps {
  notifications: Notification[]
  isLoading: boolean
  onRefetch: () => void
}

export function NotificationList({ notifications, isLoading, onRefetch }: NotificationListProps) {
  // Memoize sorted notifications to avoid re-sorting on every render
  const sortedNotifications = useMemo(() => {
    return [...notifications].sort((a, b) => {
      // Sort by isRead status (unread first) then by createdAt (newest first)
      if (!a.isRead && b.isRead) return -1
      if (a.isRead && !b.isRead) return 1
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  }, [notifications])

  // Loading skeleton
  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <Card
            key={i}
            className="p-4 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] animate-pulse"
          >
            <div className="flex gap-4">
              <div className="w-12 h-12 bg-gray-200 border-2 border-black" />
              <div className="flex-1 space-y-3">
                <div className="h-4 bg-gray-200 rounded w-1/3" />
                <div className="h-3 bg-gray-200 rounded w-full" />
                <div className="h-3 bg-gray-200 rounded w-2/3" />
                <div className="flex gap-2">
                  <div className="h-7 bg-gray-200 rounded w-20" />
                  <div className="h-7 bg-gray-200 rounded w-20" />
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    )
  }

  // Empty state
  if (!notifications || notifications.length === 0) {
    return (
      <Card className="p-12 border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] bg-white">
        <div className="flex flex-col items-center justify-center text-center">
          <div className="w-20 h-20 bg-gray-100 border-4 border-black flex items-center justify-center mb-6">
            <Inbox className="w-10 h-10 text-gray-400" />
          </div>
          <h3 className="text-xl font-bold mb-2">No notifications</h3>
          <p className="text-gray-600 max-w-md">
            You're all caught up! We'll notify you when there's something new.
          </p>
        </div>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      {/* Notification count */}
      <div className="flex items-center gap-2 text-sm text-gray-600">
        <Bell className="w-4 h-4" />
        <span>
          {notifications.length} notification{notifications.length !== 1 ? 's' : ''}
        </span>
        {notifications.filter(n => !n.isRead).length > 0 && (
          <span className="text-blue-600 font-semibold">
            ({notifications.filter(n => !n.isRead).length} unread)
          </span>
        )}
      </div>

      {/* Notification items */}
      <div className="space-y-3">
        {sortedNotifications.map((notification) => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            onRefetch={onRefetch}
          />
        ))}
      </div>
    </div>
  )
}
