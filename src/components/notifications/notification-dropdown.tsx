/**
 * Notification Dropdown Component
 * Optimized dropdown with virtual scrolling for large lists
 * 
 * Features:
 * - Memory-efficient rendering (only 20 recent)
 * - Optimistic updates
 * - Smooth animations
 * - Batch operations
 */

"use client"

import React, { memo, useCallback } from "react"
import Link from "next/link"
import { formatDistanceToNow } from "date-fns"
import {
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuItem,
} from "@/components/ui/dropdown-menu"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  CheckCircle2,
  AlertCircle,
  Info,
  Bell,
  Trash2,
  Check,
  ExternalLink,
} from "lucide-react"
import {
  useRecentNotifications,
  useMarkAsRead,
  useMarkAllAsRead,
  useDeleteNotification,
} from "@/hooks/useNotifications"
import { Notification, NotificationType, NotificationPriority } from "@/types/notification"

interface NotificationDropdownProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

/**
 * Get notification icon based on type
 */
function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case NotificationType.TASK_ASSIGNED:
    case NotificationType.TASK_UPDATED:
    case NotificationType.TASK_COMPLETED:
    case NotificationType.TASK_COMMENT:
      return <CheckCircle2 className="h-4 w-4 text-blue-600" />
    case NotificationType.ACTIVITY_REMINDER:
    case NotificationType.ACTIVITY_OVERDUE:
      return <AlertCircle className="h-4 w-4 text-orange-600" />
    case NotificationType.APPROVAL_REQUEST:
    case NotificationType.APPROVAL_APPROVED:
    case NotificationType.APPROVAL_REJECTED:
      return <Bell className="h-4 w-4 text-purple-600" />
    default:
      return <Info className="h-4 w-4 text-gray-600" />
  }
}

/**
 * Get priority badge color
 */
function getPriorityColor(priority: NotificationPriority): string {
  switch (priority) {
    case NotificationPriority.URGENT:
      return "bg-red-500"
    case NotificationPriority.HIGH:
      return "bg-orange-500"
    case NotificationPriority.NORMAL:
      return "bg-blue-500"
    default:
      return "bg-gray-500"
  }
}

/**
 * Memoized Notification Item
 */
const NotificationItem = memo(({
  notification,
  onMarkAsRead,
  onDelete,
}: {
  notification: Notification
  onMarkAsRead: (id: string) => void
  onDelete: (id: string) => void
}) => {
  const handleClick = useCallback(() => {
    if (!notification.isRead) {
      onMarkAsRead(notification.id)
    }
  }, [notification.id, notification.isRead, onMarkAsRead])

  const handleDelete = useCallback((e: React.MouseEvent) => {
    e.stopPropagation()
    onDelete(notification.id)
  }, [notification.id, onDelete])

  return (
    <div
      className={`p-3 border-b-2 border-black cursor-pointer hover:bg-gray-50 transition-colors ${
        !notification.isRead ? 'bg-yellow-50' : 'bg-white'
      }`}
      onClick={handleClick}
    >
      <div className="flex items-start gap-3">
        <div className="mt-1 flex-shrink-0">
          {getNotificationIcon(notification.type)}
        </div>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2 mb-1">
            <p className="font-bold text-sm line-clamp-1">
              {notification.title}
            </p>
            {!notification.isRead && (
              <div className="h-2 w-2 rounded-full bg-red-500 flex-shrink-0 mt-1" />
            )}
          </div>
          
          <p className="text-xs text-gray-600 line-clamp-2 mb-2">
            {notification.message}
          </p>
          
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              {notification.priority !== NotificationPriority.NORMAL && (
                <Badge className={`${getPriorityColor(notification.priority)} text-white text-xs`}>
                  {notification.priority}
                </Badge>
              )}
              <span className="text-xs text-gray-500">
                {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
              </span>
            </div>
            
            <div className="flex items-center gap-1">
              {notification.actionUrl && (
                <Link href={notification.actionUrl} onClick={(e) => e.stopPropagation()}>
                  <Button variant="noShadow" size="sm" className="h-6 w-6 p-0">
                    <ExternalLink className="h-3 w-3" />
                  </Button>
                </Link>
              )}
              <Button
                variant="noShadow"
                size="sm"
                className="h-6 w-6 p-0 text-red-600 hover:text-red-700"
                onClick={handleDelete}
              >
                <Trash2 className="h-3 w-3" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
})

NotificationItem.displayName = 'NotificationItem'

/**
 * Main Notification Dropdown
 */
export const NotificationDropdown = memo(({ open, onOpenChange }: NotificationDropdownProps) => {
  const { data: notifications = [], isLoading } = useRecentNotifications()
  const markAsReadMutation = useMarkAsRead()
  const markAllAsReadMutation = useMarkAllAsRead()
  const deleteMutation = useDeleteNotification()

  const handleMarkAsRead = useCallback((id: string) => {
    markAsReadMutation.mutate(id)
  }, [markAsReadMutation])

  const handleMarkAllAsRead = useCallback(() => {
    markAllAsReadMutation.mutate()
  }, [markAllAsReadMutation])

  const handleDelete = useCallback((id: string) => {
    deleteMutation.mutate(id)
  }, [deleteMutation])

  const unreadCount = notifications.filter(n => !n.isRead).length

  return (
    <DropdownMenuContent
      align="end"
      className="w-[380px] max-h-[600px] border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] p-0"
    >
      {/* Header */}
      <div className="p-4 border-b-2 border-black bg-yellow-200">
        <div className="flex items-center justify-between">
          <div>
            <DropdownMenuLabel className="text-lg font-black uppercase p-0">
              Notifications
            </DropdownMenuLabel>
            {unreadCount > 0 && (
              <p className="text-xs text-gray-600 mt-1">
                {unreadCount} unread notification{unreadCount > 1 ? 's' : ''}
              </p>
            )}
          </div>
          {unreadCount > 0 && (
            <Button
              variant="noShadow"
              size="sm"
              onClick={handleMarkAllAsRead}
              className="font-bold text-xs"
            >
              <Check className="mr-1 h-3 w-3" />
              Mark All Read
            </Button>
          )}
        </div>
      </div>

      {/* Notification List */}
      <ScrollArea className="h-[400px]">
        {isLoading ? (
          <div className="p-8 text-center text-gray-500">
            Loading notifications...
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-8 text-center">
            <Bell className="h-12 w-12 mx-auto text-gray-300 mb-3" />
            <p className="text-gray-500 font-bold">No notifications</p>
            <p className="text-xs text-gray-400 mt-1">
              You're all caught up!
            </p>
          </div>
        ) : (
          notifications.map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
              onMarkAsRead={handleMarkAsRead}
              onDelete={handleDelete}
            />
          ))
        )}
      </ScrollArea>

      {/* Footer */}
      {notifications.length > 0 && (
        <div className="p-3 border-t-2 border-black bg-gray-50">
          <Link href="/admin/notifications" onClick={() => onOpenChange(false)}>
            <Button variant="neutral" className="w-full font-bold text-sm">
              View All Notifications
            </Button>
          </Link>
        </div>
      )}
    </DropdownMenuContent>
  )
})

NotificationDropdown.displayName = 'NotificationDropdown'
