/**
 * Task Priority Badge Component
 * Displays task priority with color coding
 */

import { TaskPriority } from '@/types/task'
import { AlertCircle, ArrowUp, Minus, ArrowDown, Flame } from 'lucide-react'

interface TaskPriorityBadgeProps {
  priority: TaskPriority
  className?: string
}

export function TaskPriorityBadge({ priority, className = '' }: TaskPriorityBadgeProps) {
  const getPriorityConfig = (priority: TaskPriority) => {
    switch (priority) {
      case TaskPriority.CRITICAL:
        return { label: 'Critical', icon: Flame, bgColor: 'bg-red-400', textColor: 'text-red-900' }
      case TaskPriority.URGENT:
        return { label: 'Urgent', icon: AlertCircle, bgColor: 'bg-red-200', textColor: 'text-red-900' }
      case TaskPriority.HIGH:
        return { label: 'High', icon: ArrowUp, bgColor: 'bg-orange-200', textColor: 'text-orange-900' }
      case TaskPriority.MEDIUM:
        return { label: 'Medium', icon: Minus, bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
      case TaskPriority.LOW:
        return { label: 'Low', icon: ArrowDown, bgColor: 'bg-green-200', textColor: 'text-green-900' }
      default:
        return { label: priority, icon: Minus, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
    }
  }

  const config = getPriorityConfig(priority)
  const Icon = config.icon

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border-2 border-black px-3 py-1 text-xs font-black uppercase ${config.bgColor} ${config.textColor} ${className}`}
    >
      <Icon className="h-3 w-3" />
      {config.label}
    </span>
  )
}
