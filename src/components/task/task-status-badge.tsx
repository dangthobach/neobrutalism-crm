/**
 * Task Status Badge Component
 * Displays task status with color coding
 */

import { TaskStatus } from '@/types/task'

interface TaskStatusBadgeProps {
  status: TaskStatus
  className?: string
}

export function TaskStatusBadge({ status, className = '' }: TaskStatusBadgeProps) {
  const getStatusConfig = (status: TaskStatus) => {
    switch (status) {
      case TaskStatus.TODO:
        return { label: 'To Do', bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      case TaskStatus.IN_PROGRESS:
        return { label: 'In Progress', bgColor: 'bg-blue-200', textColor: 'text-blue-900' }
      case TaskStatus.IN_REVIEW:
        return { label: 'In Review', bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
      case TaskStatus.COMPLETED:
        return { label: 'Completed', bgColor: 'bg-green-200', textColor: 'text-green-900' }
      case TaskStatus.CANCELLED:
        return { label: 'Cancelled', bgColor: 'bg-red-200', textColor: 'text-red-900' }
      case TaskStatus.ON_HOLD:
        return { label: 'On Hold', bgColor: 'bg-orange-200', textColor: 'text-orange-900' }
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
