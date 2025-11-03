/**
 * Content Status Badge Component
 * Displays content status with Neobrutalism styling
 */

import { ContentStatus } from '@/types/content'

interface ContentStatusBadgeProps {
  status: ContentStatus
  className?: string
}

export function ContentStatusBadge({ status, className = '' }: ContentStatusBadgeProps) {
  const getStatusConfig = (status: ContentStatus) => {
    switch (status) {
      case ContentStatus.DRAFT:
        return {
          label: 'Draft',
          bgColor: 'bg-gray-200',
          textColor: 'text-gray-800',
        }
      case ContentStatus.REVIEW:
        return {
          label: 'In Review',
          bgColor: 'bg-yellow-300',
          textColor: 'text-yellow-900',
        }
      case ContentStatus.PUBLISHED:
        return {
          label: 'Published',
          bgColor: 'bg-green-300',
          textColor: 'text-green-900',
        }
      case ContentStatus.ARCHIVED:
        return {
          label: 'Archived',
          bgColor: 'bg-blue-300',
          textColor: 'text-blue-900',
        }
      case ContentStatus.DELETED:
        return {
          label: 'Deleted',
          bgColor: 'bg-red-300',
          textColor: 'text-red-900',
        }
      default:
        return {
          label: status,
          bgColor: 'bg-gray-200',
          textColor: 'text-gray-800',
        }
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
