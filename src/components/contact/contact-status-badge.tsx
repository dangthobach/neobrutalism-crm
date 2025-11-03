/**
 * Contact Status Badge Component
 * Displays contact status with color coding
 */

import { ContactStatus } from '@/types/contact'

interface ContactStatusBadgeProps {
  status: ContactStatus
  className?: string
}

export function ContactStatusBadge({ status, className = '' }: ContactStatusBadgeProps) {
  const getStatusConfig = (status: ContactStatus) => {
    switch (status) {
      case ContactStatus.ACTIVE:
        return { label: 'Active', bgColor: 'bg-green-200', textColor: 'text-green-900' }
      case ContactStatus.INACTIVE:
        return { label: 'Inactive', bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      case ContactStatus.DO_NOT_CONTACT:
        return { label: 'Do Not Contact', bgColor: 'bg-red-200', textColor: 'text-red-900' }
      case ContactStatus.BOUNCED:
        return { label: 'Bounced', bgColor: 'bg-orange-200', textColor: 'text-orange-900' }
      case ContactStatus.UNSUBSCRIBED:
        return { label: 'Unsubscribed', bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
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
