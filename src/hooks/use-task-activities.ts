/**
 * Task Activity Timeline React Query Hooks
 * Provides data fetching for task activity history
 */

import { useQuery } from '@tanstack/react-query'

// Types
export interface TaskActivity {
  id: string
  taskId: string
  activityType: string
  description: string
  userId: string
  username: string
  metadata: Record<string, any>
  createdAt: string
}

// API functions
const activityApi = {
  getAll: async (taskId: string): Promise<TaskActivity[]> => {
    const response = await fetch(`/api/tasks/${taskId}/activities`)
    if (!response.ok) throw new Error('Failed to fetch activities')
    const data = await response.json()
    return data.data
  },

  getPaginated: async (taskId: string, page = 0, size = 20) => {
    const response = await fetch(`/api/tasks/${taskId}/activities/paginated?page=${page}&size=${size}`)
    if (!response.ok) throw new Error('Failed to fetch activities')
    const data = await response.json()
    return data.data
  },

  getCount: async (taskId: string): Promise<number> => {
    const response = await fetch(`/api/tasks/${taskId}/activities/count`)
    if (!response.ok) throw new Error('Failed to fetch activity count')
    const data = await response.json()
    return data.data
  },
}

// Query keys
export const activityKeys = {
  all: ['activities'] as const,
  byTask: (taskId: string) => [...activityKeys.all, 'task', taskId] as const,
  count: (taskId: string) => [...activityKeys.all, 'count', taskId] as const,
}

/**
 * Get all activities for a task
 */
export function useTaskActivities(taskId: string | undefined) {
  return useQuery({
    queryKey: activityKeys.byTask(taskId!),
    queryFn: () => activityApi.getAll(taskId!),
    enabled: !!taskId,
  })
}

/**
 * Get activity count for a task
 */
export function useActivityCount(taskId: string | undefined) {
  return useQuery({
    queryKey: activityKeys.count(taskId!),
    queryFn: () => activityApi.getCount(taskId!),
    enabled: !!taskId,
  })
}
