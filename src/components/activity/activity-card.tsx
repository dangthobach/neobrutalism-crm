/**
 * Activity Card Component
 * Displays activity information in a card with Neobrutalism design
 */

'use client'

import Link from 'next/link'
import { Calendar, Clock, MapPin, User, Building2, UserCircle } from 'lucide-react'
import { Activity } from '@/types/activity'
import { ActivityTypeBadge } from './activity-type-badge'
import { ActivityStatusBadge } from './activity-status-badge'
import { formatDate, formatDateTime } from '@/lib/utils'

interface ActivityCardProps {
  activity: Activity
  onClick?: () => void
}

export function ActivityCard({ activity, onClick }: ActivityCardProps) {
  const content = (
    <>
      {/* Header */}
      <div className="flex items-center justify-between border-b-2 border-black bg-blue-200 px-4 py-3">
        <div className="flex-1">
          <h3 className="font-black uppercase leading-tight">{activity.subject}</h3>
          <div className="mt-1 flex items-center gap-2">
            <ActivityTypeBadge type={activity.type} />
          </div>
        </div>
        <ActivityStatusBadge status={activity.status} />
      </div>

      {/* Content */}
      <div className="space-y-3 p-4">
        {/* Description */}
        {activity.description && (
          <p className="text-sm font-medium text-gray-700">{activity.description}</p>
        )}

        {/* Related To */}
        <div className="space-y-1">
          {activity.customerName && (
            <div className="flex items-center gap-2 text-sm">
              <Building2 className="h-4 w-4 text-gray-500" />
              <span className="font-bold">{activity.customerName}</span>
            </div>
          )}
          {activity.contactName && (
            <div className="flex items-center gap-2 text-sm">
              <UserCircle className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{activity.contactName}</span>
            </div>
          )}
        </div>

        {/* Schedule */}
        <div className="space-y-1 border-t-2 border-black pt-2">
          {activity.scheduledAt && (
            <div className="flex items-center gap-2 text-sm">
              <Calendar className="h-4 w-4 text-gray-500" />
              <span className="font-bold">{formatDateTime(activity.scheduledAt)}</span>
            </div>
          )}
          {activity.duration && (
            <div className="flex items-center gap-2 text-sm">
              <Clock className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{activity.duration} minutes</span>
            </div>
          )}
          {activity.location && (
            <div className="flex items-center gap-2 text-sm">
              <MapPin className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{activity.location}</span>
            </div>
          )}
        </div>

        {/* Assigned To */}
        {activity.assignedToName && (
          <div className="flex items-center gap-2 border-t-2 border-black pt-2 text-sm">
            <User className="h-4 w-4 text-gray-500" />
            <span className="font-bold uppercase text-gray-500">Assigned to: </span>
            <span className="font-bold">{activity.assignedToName}</span>
          </div>
        )}

        {/* Outcome */}
        {activity.outcome && (
          <div className="rounded border-2 border-black bg-green-100 p-2">
            <p className="text-xs font-bold uppercase text-gray-600">Outcome</p>
            <p className="text-sm font-medium">{activity.outcome}</p>
          </div>
        )}
      </div>
    </>
  )

  if (onClick) {
    return (
      <div
        onClick={onClick}
        className="group block cursor-pointer transform border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
      >
        {content}
      </div>
    )
  }

  return (
    <Link
      href={`/admin/activities/${activity.id}`}
      className="group block transform border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
    >
      {content}
    </Link>
  )
}
