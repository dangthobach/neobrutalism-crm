/**
 * Activity List Component
 * Displays activities in a list with filtering
 */

'use client'

import { Activity } from '@/types/activity'
import { ActivityCard } from './activity-card'

interface ActivityListProps {
  activities: Activity[]
  isLoading?: boolean
  onActivityClick?: (activity: Activity) => void
}

export function ActivityList({ activities, isLoading, onActivityClick }: ActivityListProps) {
  if (isLoading) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center">
        <p className="font-bold uppercase">Loading activities...</p>
      </div>
    )
  }

  if (activities.length === 0) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center">
        <p className="font-bold uppercase text-gray-500">No activities found</p>
      </div>
    )
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      {activities.map((activity) => (
        <ActivityCard
          key={activity.id}
          activity={activity}
          onClick={onActivityClick ? () => onActivityClick(activity) : undefined}
        />
      ))}
    </div>
  )
}
