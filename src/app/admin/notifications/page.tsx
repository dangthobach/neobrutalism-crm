"use client"

import { useState, useEffect } from 'react'
import { useNotifications, useMarkAllAsRead } from '@/hooks/useNotifications'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { CheckCheck, Filter, Settings, Bell, Loader2 } from 'lucide-react'
import { NotificationType, NotificationPriority, type Notification } from '@/types/notification'
import { useRouter } from 'next/navigation'
import { NotificationList } from '@/components/notifications/notification-list'
import { WebSocketStatus } from '@/components/notifications/websocket-status'
import { toast } from 'sonner'

// Type labels for dropdown - using actual enum values
const TYPE_OPTIONS: Array<{ value: NotificationType | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Types' },
  { value: NotificationType.SYSTEM, label: 'System' },
  { value: NotificationType.TASK_ASSIGNED, label: 'Task Assigned' },
  { value: NotificationType.TASK_UPDATED, label: 'Task Updated' },
  { value: NotificationType.TASK_COMPLETED, label: 'Task Completed' },
  { value: NotificationType.TASK_COMMENT, label: 'Task Comment' },
  { value: NotificationType.CUSTOMER_CREATED, label: 'Customer Created' },
  { value: NotificationType.CUSTOMER_UPDATED, label: 'Customer Updated' },
  { value: NotificationType.ACTIVITY_REMINDER, label: 'Activity Reminder' },
  { value: NotificationType.ACTIVITY_OVERDUE, label: 'Activity Overdue' },
  { value: NotificationType.MENTION, label: 'Mention' },
  { value: NotificationType.APPROVAL_REQUEST, label: 'Approval Request' },
  { value: NotificationType.APPROVAL_APPROVED, label: 'Approved' },
  { value: NotificationType.APPROVAL_REJECTED, label: 'Rejected' },
]

const PRIORITY_OPTIONS: Array<{ value: NotificationPriority | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Priorities' },
  { value: NotificationPriority.LOW, label: 'Low' },
  { value: NotificationPriority.NORMAL, label: 'Normal' },
  { value: NotificationPriority.HIGH, label: 'High' },
  { value: NotificationPriority.URGENT, label: 'Urgent' },
]

export default function NotificationsPage() {
  const router = useRouter()
  const [activeTab, setActiveTab] = useState<'all' | 'unread' | 'archived'>('all')
  const [filterType, setFilterType] = useState<NotificationType | 'ALL'>('ALL')
  const [filterPriority, setFilterPriority] = useState<NotificationPriority | 'ALL'>('ALL')
  const [page, setPage] = useState(1)
  const [accumulatedNotifications, setAccumulatedNotifications] = useState<Notification[]>([])

  // Fetch notifications with pagination
  const { data, isLoading, refetch, isFetching } = useNotifications({
    page,
    size: 20, // Load 20 at a time for better UX
    isRead: activeTab === 'unread' ? false : undefined,
    isArchived: activeTab === 'archived' ? true : activeTab === 'all' ? false : undefined,
    type: filterType !== 'ALL' ? filterType : undefined,
    priority: filterPriority !== 'ALL' ? filterPriority : undefined,
  })

  const markAllMutation = useMarkAllAsRead()

  // Accumulate notifications when loading more pages
  useEffect(() => {
    if (data?.content) {
      if (page === 1) {
        // Reset for first page
        setAccumulatedNotifications(data.content)
      } else {
        // Append for subsequent pages (avoid duplicates)
        setAccumulatedNotifications((prev) => {
          const existing = new Set(prev.map(n => n.id))
          const newItems = data.content.filter((n: Notification) => !existing.has(n.id))
          return [...prev, ...newItems]
        })
      }
    }
  }, [data, page])

  // Reset page when filters change
  useEffect(() => {
    setPage(1)
    setAccumulatedNotifications([])
  }, [activeTab, filterType, filterPriority])

  const totalPages = data?.totalPages || 0
  const hasMore = page < totalPages

  const handleMarkAllAsRead = async () => {
    try {
      await markAllMutation.mutateAsync()
      toast.success('All notifications marked as read')
      setPage(1)
      refetch()
    } catch (error) {
      toast.error('Failed to mark all as read')
    }
  }

  const handleLoadMore = () => {
    if (!isFetching && hasMore) {
      setPage((prev) => prev + 1)
    }
  }

  const unreadCount = accumulatedNotifications.filter((n) => !n.isRead).length

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto py-6 px-4">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-blue-500 border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <Bell className="w-8 h-8 text-white" />
              </div>
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-4xl font-black">Notifications</h1>
                  <WebSocketStatus showReconnect />
                </div>
                <p className="text-gray-600 font-medium">
                  {unreadCount > 0 ? (
                    <span className="text-blue-600 font-bold">{unreadCount} unread notification(s)</span>
                  ) : (
                    <span>All caught up!</span>
                  )}
                </p>
              </div>
            </div>

            <div className="flex gap-2">
              <Button
                onClick={handleMarkAllAsRead}
                disabled={unreadCount === 0 || markAllMutation.isPending}
                className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
              >
                <CheckCheck className="w-4 h-4 mr-2" />
                Mark All as Read
              </Button>

              <Button
                onClick={() => router.push('/admin/notifications/preferences')}
                className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
              >
                <Settings className="w-4 h-4 mr-2" />
                Preferences
              </Button>
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] p-4 mb-6">
          <div className="flex items-center gap-4 flex-wrap">
            <div className="flex items-center gap-2">
              <Filter className="w-5 h-5" />
              <span className="font-bold">Filters:</span>
            </div>

            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value as NotificationType | 'ALL')}
              className="px-3 py-2 border-2 border-black rounded font-medium bg-white hover:bg-gray-50 transition-colors"
            >
              {TYPE_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>

            <select
              value={filterPriority}
              onChange={(e) => setFilterPriority(e.target.value as NotificationPriority | 'ALL')}
              className="px-3 py-2 border-2 border-black rounded font-medium bg-white hover:bg-gray-50 transition-colors"
            >
              {PRIORITY_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>

            {(filterType !== 'ALL' || filterPriority !== 'ALL') && (
              <Button
                size="sm"
                onClick={() => {
                  setFilterType('ALL')
                  setFilterPriority('ALL')
                }}
                className="text-blue-600 hover:text-blue-800 underline"
              >
                Clear Filters
              </Button>
            )}
          </div>
        </div>

        {/* Tabs */}
        <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)} className="w-full">
          <TabsList className="grid w-full grid-cols-3 mb-6 bg-gray-100 p-1 border-2 border-black">
            <TabsTrigger
              value="all"
              className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black font-bold"
            >
              All
            </TabsTrigger>
            <TabsTrigger
              value="unread"
              className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black font-bold"
            >
              Unread {unreadCount > 0 && `(${unreadCount})`}
            </TabsTrigger>
            <TabsTrigger
              value="archived"
              className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black font-bold"
            >
              Archived
            </TabsTrigger>
          </TabsList>

          <TabsContent value={activeTab}>
            <NotificationList
              notifications={accumulatedNotifications}
              isLoading={isLoading}
              onRefetch={refetch}
            />

            {/* Load More Button */}
            {hasMore && !isLoading && (
              <div className="mt-6 flex justify-center">
                <Button
                  onClick={handleLoadMore}
                  disabled={isFetching}
                  className="border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] transition-all"
                >
                  {isFetching ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Loading...
                    </>
                  ) : (
                    <>Load More ({data?.totalElements ? data.totalElements - accumulatedNotifications.length : 0} remaining)</>
                  )}
                </Button>
              </div>
            )}

            {/* Pagination Info */}
            {data?.totalElements && (
              <div className="mt-4 text-center text-sm text-gray-600">
                Showing {accumulatedNotifications.length} of {data.totalElements} notifications
                {hasMore && ' • Page ' + page + ' of ' + totalPages}
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
