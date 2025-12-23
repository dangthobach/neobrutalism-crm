/**
 * Activity Status Badge Component
 * Displays activity status with color coding
 */

import { ActivityStatus } from '@/types/activity'

interface ActivityStatusBadgeProps {
  status: ActivityStatus
  className?: string
}

export function ActivityStatusBadge({ status, className = '' }: ActivityStatusBadgeProps) {
  const getStatusConfig = (status: ActivityStatus) => {
    switch (status) {
      case ActivityStatus.SCHEDULED:
        return { label: 'Scheduled', bgColor: 'bg-blue-200', textColor: 'text-blue-900' }
      case ActivityStatus.IN_PROGRESS:
        return { label: 'In Progress', bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
      case ActivityStatus.COMPLETED:
        return { label: 'Completed', bgColor: 'bg-green-200', textColor: 'text-green-900' }
      case ActivityStatus.CANCELLED:
        return { label: 'Cancelled', bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      case ActivityStatus.OVERDUE:
        return { label: 'Overdue', bgColor: 'bg-red-200', textColor: 'text-red-900' }
      default:
        return { label: status, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
    }
  }

  const config = getStatusConfig(status)

  return (
    <span
      className={`inline-flex items-center rounded-full border-2 border-black px-3 py-1 text-xs font-black uppercase ${config.bgColor} ${config.textColor} ${className}`}
    >
      {config.label}
    </span>
  )
}
