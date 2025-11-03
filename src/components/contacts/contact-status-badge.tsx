/**
 * Contact Status Badge Component
 * Displays contact status with appropriate styling
 */

import { Badge } from '@/components/ui/badge'
import { ContactStatus } from '@/types/contact'
import { cn } from '@/lib/utils'

interface ContactStatusBadgeProps {
  status: ContactStatus
  className?: string
}

export function ContactStatusBadge({ status, className }: ContactStatusBadgeProps) {
  const variants: Record<ContactStatus, { label: string; className?: string }> = {
    [ContactStatus.ACTIVE]: {
      label: 'Active',
      className: 'bg-green-100 text-green-800 border-green-300',
    },
    [ContactStatus.INACTIVE]: {
      label: 'Inactive',
      className: 'bg-gray-100 text-gray-800 border-gray-300',
    },
    [ContactStatus.DO_NOT_CONTACT]: {
      label: 'Do Not Contact',
      className: 'bg-red-100 text-red-800 border-red-300',
    },
    [ContactStatus.BOUNCED]: {
      label: 'Bounced',
      className: 'bg-orange-100 text-orange-800 border-orange-300',
    },
    [ContactStatus.UNSUBSCRIBED]: {
      label: 'Unsubscribed',
      className: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    },
  }

  const config = variants[status]

  return (
    <Badge className={cn('border-2 border-black', config.className, className)}>
      {config.label}
    </Badge>
  )
}
