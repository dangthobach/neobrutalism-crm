/**
 * Contact Role Badge Component
 * Displays contact role with color coding
 */

import { ContactRole } from '@/types/contact'

interface ContactRoleBadgeProps {
  role: ContactRole
  className?: string
}

export function ContactRoleBadge({ role, className = '' }: ContactRoleBadgeProps) {
  const getRoleConfig = (role: ContactRole) => {
    switch (role) {
      case ContactRole.DECISION_MAKER:
        return { label: 'Decision Maker', bgColor: 'bg-purple-200', textColor: 'text-purple-900' }
      case ContactRole.INFLUENCER:
        return { label: 'Influencer', bgColor: 'bg-blue-200', textColor: 'text-blue-900' }
      case ContactRole.CHAMPION:
        return { label: 'Champion', bgColor: 'bg-green-200', textColor: 'text-green-900' }
      case ContactRole.EVALUATOR:
        return { label: 'Evaluator', bgColor: 'bg-yellow-200', textColor: 'text-yellow-900' }
      case ContactRole.GATEKEEPER:
        return { label: 'Gatekeeper', bgColor: 'bg-orange-200', textColor: 'text-orange-900' }
      case ContactRole.USER:
        return { label: 'User', bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      case ContactRole.OTHER:
        return { label: 'Other', bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
      default:
        return { label: role, bgColor: 'bg-gray-200', textColor: 'text-gray-900' }
    }
  }

  const config = getRoleConfig(role)

  return (
    <span
      className={`inline-flex items-center rounded-full border-2 border-black px-3 py-1 text-xs font-black uppercase ${config.bgColor} ${config.textColor} ${className}`}
    >
      {config.label}
    </span>
  )
}
