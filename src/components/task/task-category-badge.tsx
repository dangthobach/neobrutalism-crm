/**
 * Task Category Badge Component
 * Displays task category with color coding
 */

import { TaskCategory } from '@/types/task'
import { TrendingUp, Headphones, UserPlus, Phone, Users, Search, FileSignature, FileCheck, MoreHorizontal } from 'lucide-react'

interface TaskCategoryBadgeProps {
  category: TaskCategory
  className?: string
}

export function TaskCategoryBadge({ category, className = '' }: TaskCategoryBadgeProps) {
  const getCategoryConfig = (category: TaskCategory) => {
    switch (category) {
      case TaskCategory.SALES:
        return { label: 'Sales', icon: TrendingUp, bgColor: 'bg-green-200', textColor: 'text-green-900' }
      case TaskCategory.SUPPORT:
        return { label: 'Support', icon: Headphones, bgColor: 'bg-blue-200', textColor: 'text-blue-900' }
      case TaskCategory.ONBOARDING:
        return { label: 'Onboarding', icon: UserPlus, bgColor: 'bg-purple-200', textColor: 'text-purple-900' }
      case TaskCategory.FOLLOW_UP:
        return { label: 'Follow Up', icon: Phone, bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
      case TaskCategory.MEETING:
        return { label: 'Meeting', icon: Users, bgColor: 'bg-pink-200', textColor: 'text-pink-900' }
      case TaskCategory.RESEARCH:
        return { label: 'Research', icon: Search, bgColor: 'bg-indigo-200', textColor: 'text-indigo-900' }
      case TaskCategory.PROPOSAL:
        return { label: 'Proposal', icon: FileSignature, bgColor: 'bg-orange-200', textColor: 'text-orange-900' }
      case TaskCategory.CONTRACT:
        return { label: 'Contract', icon: FileCheck, bgColor: 'bg-teal-200', textColor: 'text-teal-900' }
      case TaskCategory.OTHER:
        return { label: 'Other', icon: MoreHorizontal, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      default:
        return { label: category, icon: MoreHorizontal, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
    }
  }

  const config = getCategoryConfig(category)
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
