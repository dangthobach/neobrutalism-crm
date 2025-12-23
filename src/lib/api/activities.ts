/**
 * Activity API Client
 * Handles all activity-related API calls
 */

import { apiClient, PageResponse } from './client'
import type {
  Activity,
  CreateActivityRequest,
  UpdateActivityRequest,
  ActivitySearchParams,
  ActivityStats,
} from '@/types/activity'

export const activityApi = {
  /**
   * Get paginated list of activities
   */
  getAll: async (params?: ActivitySearchParams): Promise<PageResponse<Activity>> => {
    const response = await apiClient.get<PageResponse<Activity>>('/activities', params)
    return response as any
  },

  /**
   * Get activity by ID
   */
  getById: async (id: string): Promise<Activity> => {
    const response = await apiClient.get<Activity>(`/activities/${id}`)
    return response as any
  },

  /**
   * Create new activity
   */
  create: async (data: CreateActivityRequest): Promise<Activity> => {
    const response = await apiClient.post<Activity>('/activities', data)
    return response as any
  },

  /**
   * Update activity
   */
  update: async (id: string, data: UpdateActivityRequest): Promise<Activity> => {
    const response = await apiClient.put<Activity>(`/activities/${id}`, data)
    return response as any
  },

  /**
   * Delete activity
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/activities/${id}`)
  },

  /**
   * Get activities by customer
   */
  getByCustomer: async (customerId: string, params?: ActivitySearchParams): Promise<PageResponse<Activity>> => {
    const response = await apiClient.get<PageResponse<Activity>>(`/activities/customer/${customerId}`, params)
    return response as any
  },

  /**
   * Get activities by contact
   */
  getByContact: async (contactId: string, params?: ActivitySearchParams): Promise<PageResponse<Activity>> => {
    const response = await apiClient.get<PageResponse<Activity>>(`/activities/contact/${contactId}`, params)
    return response as any
  },

  /**
   * Get activities assigned to user
   */
  getByAssignee: async (userId: string, params?: ActivitySearchParams): Promise<PageResponse<Activity>> => {
    const response = await apiClient.get<PageResponse<Activity>>(`/activities/assigned/${userId}`, params)
    return response as any
  },

  /**
   * Get today's activities
   */
  getToday: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/today', params)
    return response as any
  },

  /**
   * Get this week's activities
   */
  getThisWeek: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/this-week', params)
    return response as any
  },

  /**
   * Get upcoming activities
   */
  getUpcoming: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/upcoming', params)
    return response as any
  },

  /**
   * Get overdue activities
   */
  getOverdue: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/overdue', params)
    return response as any
  },

  /**
   * Mark activity as completed
   */
  complete: async (id: string, outcome?: string): Promise<Activity> => {
    const endpoint = outcome 
      ? `/activities/${id}/complete?outcome=${encodeURIComponent(outcome)}`
      : `/activities/${id}/complete`
    const response = await apiClient.post<Activity>(endpoint)
    return response as any
  },

  /**
   * Cancel activity
   */
  cancel: async (id: string, reason?: string): Promise<Activity> => {
    const endpoint = reason 
      ? `/activities/${id}/cancel?reason=${encodeURIComponent(reason)}`
      : `/activities/${id}/cancel`
    const response = await apiClient.post<Activity>(endpoint)
    return response as any
  },

  /**
   * Reschedule activity
   */
  reschedule: async (id: string, newDate: string): Promise<Activity> => {
    const response = await apiClient.post<Activity>(`/activities/${id}/reschedule`, { scheduledAt: newDate })
    return response as any
  },

  /**
   * Get activity statistics
   */
  getStats: async (): Promise<ActivityStats> => {
    const response = await apiClient.get<ActivityStats>('/activities/stats')
    return response as any
  },

  /**
   * Search activities
   */
  search: async (keyword: string): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/search', { keyword })
    return response as any
  },
}
