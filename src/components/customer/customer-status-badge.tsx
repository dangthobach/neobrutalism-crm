/**
 * Customer Status Badge Component
 * Displays customer status with color coding
 */

import { CustomerStatus } from '@/types/customer'

interface CustomerStatusBadgeProps {
  status: CustomerStatus
  className?: string
}

export function CustomerStatusBadge({ status, className = '' }: CustomerStatusBadgeProps) {
  const getStatusConfig = (status: CustomerStatus) => {
    switch (status) {
      case CustomerStatus.ACTIVE:
        return {
          label: 'Active',
          bgColor: 'bg-green-200',
          textColor: 'text-green-900',
        }
      case CustomerStatus.PROSPECT:
        return {
          label: 'Prospect',
          bgColor: 'bg-blue-200',
          textColor: 'text-blue-900',
        }
      case CustomerStatus.LEAD:
        return {
          label: 'Lead',
          bgColor: 'bg-yellow-200',
          textColor: 'text-yellow-900',
        }
      case CustomerStatus.INACTIVE:
        return {
          label: 'Inactive',
          bgColor: 'bg-gray-200',
          textColor: 'text-gray-900',
        }
      case CustomerStatus.CHURNED:
        return {
          label: 'Churned',
          bgColor: 'bg-orange-200',
          textColor: 'text-orange-900',
        }
      case CustomerStatus.BLACKLISTED:
        return {
          label: 'Blacklisted',
          bgColor: 'bg-red-200',
          textColor: 'text-red-900',
        }
      default:
        return {
          label: status,
          bgColor: 'bg-gray-200',
          textColor: 'text-gray-900',
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
