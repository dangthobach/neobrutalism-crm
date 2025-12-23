/**
 * Activity Timeline Component
 * Displays chronological activity feed for a task
 */

'use client'

import { useMemo } from 'react'
import { History, Loader2 } from 'lucide-react'
import { useTaskActivities } from '@/hooks/use-task-activities'
import { ActivityItem } from './activity-item'
import { Skeleton } from '@/components/ui/skeleton'
import { format, isToday, isYesterday, parseISO } from 'date-fns'
import { vi } from 'date-fns/locale'

interface ActivityTimelineProps {
  taskId: string
}

export function ActivityTimeline({ taskId }: ActivityTimelineProps) {
  const { data: activities = [], isLoading } = useTaskActivities(taskId)

  // Group activities by date
  const groupedActivities = useMemo(() => {
    const groups: Record<string, typeof activities> = {}

    activities.forEach((activity) => {
      const date = parseISO(activity.createdAt)
      let dateKey: string

      if (isToday(date)) {
        dateKey = 'Hôm nay'
      } else if (isYesterday(date)) {
        dateKey = 'Hôm qua'
      } else {
        dateKey = format(date, 'dd/MM/yyyy', { locale: vi })
      }

      if (!groups[dateKey]) {
        groups[dateKey] = []
      }
      groups[dateKey].push(activity)
    })

    return groups
  }, [activities])

  const dateKeys = Object.keys(groupedActivities)

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-20 w-full border-2 border-black" />
        <Skeleton className="h-20 w-full border-2 border-black" />
        <Skeleton className="h-20 w-full border-2 border-black" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center gap-2">
        <History className="h-5 w-5" />
        <h3 className="text-lg font-black uppercase">
          Lịch sử hoạt động ({activities.length})
        </h3>
      </div>

      {/* Timeline */}
      {activities.length === 0 ? (
        <div className="border-2 border-black bg-gray-50 p-8 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <History className="mx-auto mb-2 h-12 w-12 text-gray-400" />
          <p className="font-bold text-gray-600">Chưa có hoạt động nào</p>
          <p className="mt-1 text-sm text-gray-500">
            Các thay đổi của task sẽ được ghi lại ở đây
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {dateKeys.map((dateKey) => (
            <div key={dateKey}>
              {/* Date Header */}
              <div className="mb-3 flex items-center gap-2">
                <div className="h-px flex-1 bg-gray-300"></div>
                <span className="text-xs font-black uppercase text-gray-600">
                  {dateKey}
                </span>
                <div className="h-px flex-1 bg-gray-300"></div>
              </div>

              {/* Activities for this date */}
              <div className="space-y-4">
                {groupedActivities[dateKey].map((activity) => (
                  <ActivityItem key={activity.id} activity={activity} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Loading indicator */}
      {isLoading && (
        <div className="flex justify-center py-4">
          <Loader2 className="h-6 w-6 animate-spin text-purple-600" />
        </div>
      )}
    </div>
  )
}
