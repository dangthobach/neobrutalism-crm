/**
 * Activity Item Component
 * Displays a single activity entry with icon and metadata
 */

'use client'

import { formatDistanceToNow } from 'date-fns'
import { vi } from 'date-fns/locale'
import {
  FileText,
  CheckSquare,
  MessageSquare,
  UserPlus,
  Edit,
  Trash2,
  GitBranch,
  CheckCircle2,
} from 'lucide-react'
import { TaskActivity } from '@/hooks/use-task-activities'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'

interface ActivityItemProps {
  activity: TaskActivity
}

const activityIcons: Record<string, React.ElementType> = {
  CREATED: FileText,
  STATUS_CHANGED: GitBranch,
  ASSIGNED: UserPlus,
  COMMENT_ADDED: MessageSquare,
  CHECKLIST_UPDATED: CheckSquare,
  UPDATED: Edit,
  DELETED: Trash2,
}

const activityColors: Record<string, string> = {
  CREATED: 'bg-green-200',
  STATUS_CHANGED: 'bg-blue-200',
  ASSIGNED: 'bg-purple-200',
  COMMENT_ADDED: 'bg-yellow-200',
  CHECKLIST_UPDATED: 'bg-orange-200',
  UPDATED: 'bg-gray-200',
  DELETED: 'bg-red-200',
}

export function ActivityItem({ activity }: ActivityItemProps) {
  const Icon = activityIcons[activity.activityType] || FileText
  const iconBg = activityColors[activity.activityType] || 'bg-gray-200'

  return (
    <div className="flex gap-3">
      {/* Icon */}
      <div
        className={`flex h-10 w-10 shrink-0 items-center justify-center border-2 border-black ${iconBg} shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]`}
      >
        <Icon className="h-5 w-5" />
      </div>

      {/* Content */}
      <div className="flex-1">
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1">
            <p className="font-bold leading-tight">{activity.description}</p>

            {/* Metadata */}
            {activity.metadata && Object.keys(activity.metadata).length > 0 && (
              <div className="mt-1 space-y-1">
                {activity.metadata.oldStatus && activity.metadata.newStatus && (
                  <div className="flex items-center gap-2 text-xs font-medium text-gray-600">
                    <span className="rounded border border-gray-300 bg-gray-100 px-2 py-0.5">
                      {activity.metadata.oldStatus}
                    </span>
                    <span>→</span>
                    <span className="rounded border border-gray-300 bg-gray-100 px-2 py-0.5">
                      {activity.metadata.newStatus}
                    </span>
                  </div>
                )}

                {activity.metadata.completed !== undefined && activity.metadata.total !== undefined && (
                  <div className="text-xs font-medium text-gray-600">
                    <CheckCircle2 className="mr-1 inline h-3 w-3" />
                    {activity.metadata.completed}/{activity.metadata.total} hoàn thành
                    {activity.metadata.percentage !== undefined && (
                      <span className="ml-1 text-purple-600">
                        ({activity.metadata.percentage}%)
                      </span>
                    )}
                  </div>
                )}

                {activity.metadata.assignedToName && (
                  <div className="text-xs font-medium text-gray-600">
                    Người được giao: {activity.metadata.assignedToName}
                  </div>
                )}
              </div>
            )}

            {/* User and Time */}
            <div className="mt-2 flex items-center gap-2">
              <Avatar className="h-6 w-6 border-2 border-black">
                <AvatarFallback className="bg-purple-200 text-[10px] font-black">
                  {activity.username?.slice(0, 2).toUpperCase() || 'SY'}
                </AvatarFallback>
              </Avatar>
              <span className="text-xs font-medium text-gray-600">
                {activity.username || 'System'}
              </span>
              <span className="text-xs text-gray-400">•</span>
              <span className="text-xs text-gray-500">
                {formatDistanceToNow(new Date(activity.createdAt), {
                  addSuffix: true,
                  locale: vi,
                })}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
