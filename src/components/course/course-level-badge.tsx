'use client'

import { CourseLevel } from '@/types/course'
import { cn } from '@/lib/utils'

interface CourseLevelBadgeProps {
  level: CourseLevel
  className?: string
}

export function CourseLevelBadge({ level, className }: CourseLevelBadgeProps) {
  const variants = {
    [CourseLevel.BEGINNER]: {
      bg: 'bg-green-200',
      border: 'border-green-400',
      text: 'text-green-800',
      icon: '🌱',
    },
    [CourseLevel.INTERMEDIATE]: {
      bg: 'bg-yellow-200',
      border: 'border-yellow-400',
      text: 'text-yellow-800',
      icon: '📚',
    },
    [CourseLevel.ADVANCED]: {
      bg: 'bg-orange-200',
      border: 'border-orange-400',
      text: 'text-orange-800',
      icon: '🎯',
    },
    [CourseLevel.EXPERT]: {
      bg: 'bg-red-200',
      border: 'border-red-400',
      text: 'text-red-800',
      icon: '🏆',
    },
  }

  const variant = variants[level]

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border-2 border-black px-3 py-1 text-xs font-black uppercase',
        variant.bg,
        variant.text,
        className
      )}
    >
      <span>{variant.icon}</span>
      {level}
    </span>
  )
}
