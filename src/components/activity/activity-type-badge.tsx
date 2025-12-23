/**
 * Activity Type Badge Component
 * Displays activity type with icon and color coding
 */

import { ActivityType } from '@/types/activity'
import { Phone, Mail, Users, FileText, CheckSquare, Presentation, FileSignature, FileCheck, Headphones, MoreHorizontal } from 'lucide-react'

interface ActivityTypeBadgeProps {
  type: ActivityType
  className?: string
}

export function ActivityTypeBadge({ type, className = '' }: ActivityTypeBadgeProps) {
  const getTypeConfig = (type: ActivityType) => {
    switch (type) {
      case ActivityType.CALL:
        return { label: 'Call', icon: Phone, bgColor: 'bg-blue-200', textColor: 'text-blue-900' }
      case ActivityType.EMAIL:
        return { label: 'Email', icon: Mail, bgColor: 'bg-green-200', textColor: 'text-green-900' }
      case ActivityType.MEETING:
        return { label: 'Meeting', icon: Users, bgColor: 'bg-purple-200', textColor: 'text-purple-900' }
      case ActivityType.NOTE:
        return { label: 'Note', icon: FileText, bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
      case ActivityType.TASK:
        return { label: 'Task', icon: CheckSquare, bgColor: 'bg-orange-200', textColor: 'text-orange-900' }
      case ActivityType.DEMO:
        return { label: 'Demo', icon: Presentation, bgColor: 'bg-pink-200', textColor: 'text-pink-900' }
      case ActivityType.PROPOSAL:
        return { label: 'Proposal', icon: FileSignature, bgColor: 'bg-indigo-200', textColor: 'text-indigo-900' }
      case ActivityType.CONTRACT:
        return { label: 'Contract', icon: FileCheck, bgColor: 'bg-teal-200', textColor: 'text-teal-900' }
      case ActivityType.SUPPORT:
        return { label: 'Support', icon: Headphones, bgColor: 'bg-red-200', textColor: 'text-red-900' }
      case ActivityType.OTHER:
        return { label: 'Other', icon: MoreHorizontal, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      default:
        return { label: type, icon: MoreHorizontal, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
    }
  }

  const config = getTypeConfig(type)
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
