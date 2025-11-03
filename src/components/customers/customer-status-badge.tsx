/**
 * Customer Status Badge Component
 * Displays customer status with appropriate styling
 */

import { Badge } from '@/components/ui/badge'
import { CustomerStatus } from '@/types/customer'
import { cn } from '@/lib/utils'

interface CustomerStatusBadgeProps {
  status: CustomerStatus
  className?: string
}

export function CustomerStatusBadge({ status, className }: CustomerStatusBadgeProps) {
  const variants: Record<CustomerStatus, { variant: any; label: string; className?: string }> = {
    [CustomerStatus.LEAD]: {
      variant: 'secondary',
      label: 'Lead',
      className: 'bg-blue-100 text-blue-800 border-blue-300',
    },
    [CustomerStatus.PROSPECT]: {
      variant: 'default',
      label: 'Prospect',
      className: 'bg-purple-100 text-purple-800 border-purple-300',
    },
    [CustomerStatus.ACTIVE]: {
      variant: 'default',
      label: 'Active',
      className: 'bg-green-100 text-green-800 border-green-300',
    },
    [CustomerStatus.INACTIVE]: {
      variant: 'outline',
      label: 'Inactive',
      className: 'bg-gray-100 text-gray-800 border-gray-300',
    },
    [CustomerStatus.CHURNED]: {
      variant: 'destructive',
      label: 'Churned',
      className: 'bg-orange-100 text-orange-800 border-orange-300',
    },
    [CustomerStatus.BLACKLISTED]: {
      variant: 'destructive',
      label: 'Blacklisted',
      className: 'bg-red-100 text-red-800 border-red-300',
    },
  }

  const config = variants[status]

  return (
    <Badge
      variant={config.variant}
      className={cn(config.className, className)}
    >
      {config.label}
    </Badge>
  )
}
