'use client'

import React, { memo } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { Bell, CheckCircle, AlertCircle, Info, Calendar, Users, FileText, Settings, Archive, Trash2 } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useMarkAsRead, useArchiveNotification, useDeleteNotification } from '@/hooks/useNotifications'
import { toast } from 'sonner'
import type { Notification } from '@/types/notification'

interface NotificationItemProps {
  notification: Notification
  onRefetch: () => void
}

const NotificationItemComponent = ({ notification, onRefetch }: NotificationItemProps) => {
  const markAsReadMutation = useMarkAsRead()
  const archiveMutation = useArchiveNotification()
  const deleteMutation = useDeleteNotification()

  const getIcon = (type: string) => {
    switch (type) {
      case 'TASK_ASSIGNED':
      case 'TASK_UPDATED':
      case 'TASK_COMPLETED':
        return <FileText className="w-5 h-5" />
      case 'REMINDER':
      case 'DEADLINE':
        return <Calendar className="w-5 h-5" />
      case 'TEAM_INVITATION':
      case 'USER_MENTION':
        return <Users className="w-5 h-5" />
      case 'SYSTEM':
        return <Settings className="w-5 h-5" />
      case 'SUCCESS':
        return <CheckCircle className="w-5 h-5" />
      case 'WARNING':
      case 'ERROR':
        return <AlertCircle className="w-5 h-5" />
      default:
        return <Info className="w-5 h-5" />
    }
  }

  const getIconBgColor = (type: string) => {
    switch (type) {
      case 'TASK_COMPLETED':
      case 'SUCCESS':
        return 'bg-green-100'
      case 'WARNING':
      case 'DEADLINE':
        return 'bg-yellow-100'
      case 'ERROR':
        return 'bg-red-100'
      case 'TEAM_INVITATION':
      case 'USER_MENTION':
        return 'bg-blue-100'
      default:
        return 'bg-gray-100'
    }
  }

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return 'bg-red-500 text-white'
      case 'HIGH':
        return 'bg-orange-500 text-white'
      case 'MEDIUM':
        return 'bg-yellow-500 text-black'
      case 'LOW':
        return 'bg-gray-500 text-white'
      default:
        return 'bg-gray-300 text-black'
    }
  }

  const handleMarkAsRead = async (e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await markAsReadMutation.mutateAsync(notification.id)
      onRefetch()
    } catch (error) {
      toast.error('Failed to mark as read')
    }
  }

  const handleArchive = async (e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await archiveMutation.mutateAsync(notification.id)
      toast.success('Notification archived')
      onRefetch()
    } catch (error) {
      toast.error('Failed to archive notification')
    }
  }

  const handleDelete = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (window.confirm('Are you sure you want to delete this notification?')) {
      try {
        await deleteMutation.mutateAsync(notification.id)
        toast.success('Notification deleted')
        onRefetch()
      } catch (error) {
        toast.error('Failed to delete notification')
      }
    }
  }

  const handleClick = () => {
    // Mark as read when clicked
    if (!notification.isRead) {
      markAsReadMutation.mutate(notification.id)
    }

    // Navigate based on notification type
    if (notification.entityType === 'TASK' && notification.entityId) {
      window.location.href = `/admin/tasks/${notification.entityId}`
    } else if (notification.entityType === 'CONTACT' && notification.entityId) {
      window.location.href = `/admin/contacts/${notification.entityId}`
    } else if (notification.actionUrl) {
      window.location.href = notification.actionUrl
    }
  }

  const isUnread = !notification.isRead
  const isLoading = markAsReadMutation.isPending || archiveMutation.isPending || deleteMutation.isPending

  return (
    <Card
      className={`
        relative p-4 border-4 border-black transition-all cursor-pointer
        hover:shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
        ${isUnread ? 'bg-blue-50 shadow-[4px_4px_0px_0px_rgba(59,130,246,1)]' : 'bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]'}
        ${isLoading ? 'opacity-50 pointer-events-none' : ''}
      `}
      onClick={handleClick}
    >
      {/* Unread indicator */}
      {isUnread && (
        <div className="absolute top-4 left-0 w-1 h-12 bg-blue-500" />
      )}

      <div className="flex gap-4">
        {/* Icon */}
        <div className={`flex-shrink-0 w-12 h-12 ${getIconBgColor(notification.type)} border-2 border-black flex items-center justify-center`}>
          {getIcon(notification.type)}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          {/* Header with priority and time */}
          <div className="flex items-start justify-between gap-2 mb-2">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className={`font-bold text-sm ${isUnread ? 'text-black' : 'text-gray-900'}`}>
                {notification.title}
              </h3>
              <Badge className={`${getPriorityColor(notification.priority)} border-2 border-black text-xs px-2 py-0`}>
                {notification.priority}
              </Badge>
            </div>
            <span className="text-xs text-gray-500 whitespace-nowrap">
              {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
            </span>
          </div>

          {/* Message */}
          <p className="text-sm text-gray-700 mb-3 line-clamp-2">
            {notification.message}
          </p>

          {/* Actions */}
          <div className="flex gap-2 flex-wrap">
            {isUnread && (
              <Button
                size="sm"
                variant="neutral"
                onClick={handleMarkAsRead}
                disabled={isLoading}
                className="border-2 border-black text-xs h-7 px-2"
              >
                <CheckCircle className="w-3 h-3 mr-1" />
                Mark read
              </Button>
            )}
            <Button
              size="sm"
              variant="neutral"
              onClick={handleArchive}
              disabled={isLoading}
              className="border-2 border-black text-xs h-7 px-2"
            >
              <Archive className="w-3 h-3 mr-1" />
              Archive
            </Button>
            <Button
              size="sm"
              variant="neutral"
              onClick={handleDelete}
              disabled={isLoading}
              className="border-2 border-black text-red-600 hover:bg-red-50 text-xs h-7 px-2"
            >
              <Trash2 className="w-3 h-3 mr-1" />
              Delete
            </Button>
          </div>
        </div>
      </div>
    </Card>
  )
}

// Memoize component to prevent unnecessary re-renders (critical for 50K CCU)
export const NotificationItem = memo(NotificationItemComponent, (prevProps, nextProps) => {
  // Only re-render if notification data changes
  return (
    prevProps.notification.id === nextProps.notification.id &&
    prevProps.notification.isRead === nextProps.notification.isRead &&
    prevProps.notification.isArchived === nextProps.notification.isArchived
  )
})

NotificationItem.displayName = 'NotificationItem'
