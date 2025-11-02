'use client'

import { CourseStatus } from '@/types/course'
import { cn } from '@/lib/utils'

interface CourseStatusBadgeProps {
  status: CourseStatus
  className?: string
}

export function CourseStatusBadge({ status, className }: CourseStatusBadgeProps) {
  const variants = {
    [CourseStatus.DRAFT]: {
      bg: 'bg-gray-200',
      border: 'border-gray-400',
      text: 'text-gray-800',
    },
    [CourseStatus.PUBLISHED]: {
      bg: 'bg-green-200',
      border: 'border-green-400',
      text: 'text-green-800',
    },
    [CourseStatus.ARCHIVED]: {
      bg: 'bg-blue-200',
      border: 'border-blue-400',
      text: 'text-blue-800',
    },
  }

  const variant = variants[status]

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border-2 border-black px-3 py-1 text-xs font-black uppercase',
        variant.bg,
        variant.text,
        className
      )}
    >
      {status}
    </span>
  )
}
