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
    return response.data!
  },

  /**
   * Get activity by ID
   */
  getById: async (id: string): Promise<Activity> => {
    const response = await apiClient.get<Activity>(`/activities/${id}`)
    return response.data!
  },

  /**
   * Create new activity
   */
  create: async (data: CreateActivityRequest): Promise<Activity> => {
    const response = await apiClient.post<Activity>('/activities', data)
    return response.data!
  },

  /**
   * Update activity
   */
  update: async (id: string, data: UpdateActivityRequest): Promise<Activity> => {
    const response = await apiClient.put<Activity>(`/activities/${id}`, data)
    return response.data!
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
    return response.data!
  },

  /**
   * Get activities by contact
   */
  getByContact: async (contactId: string, params?: ActivitySearchParams): Promise<PageResponse<Activity>> => {
    const response = await apiClient.get<PageResponse<Activity>>(`/activities/contact/${contactId}`, params)
    return response.data!
  },

  /**
   * Get activities assigned to user
   */
  getByAssignee: async (userId: string, params?: ActivitySearchParams): Promise<PageResponse<Activity>> => {
    const response = await apiClient.get<PageResponse<Activity>>(`/activities/assigned/${userId}`, params)
    return response.data!
  },

  /**
   * Get today's activities
   */
  getToday: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/today', params)
    return response.data!
  },

  /**
   * Get this week's activities
   */
  getThisWeek: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/this-week', params)
    return response.data!
  },

  /**
   * Get upcoming activities
   */
  getUpcoming: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/upcoming', params)
    return response.data!
  },

  /**
   * Get overdue activities
   */
  getOverdue: async (params?: ActivitySearchParams): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/overdue', params)
    return response.data!
  },

  /**
   * Mark activity as completed
   */
  complete: async (id: string, outcome?: string): Promise<Activity> => {
    const endpoint = outcome 
      ? `/activities/${id}/complete?outcome=${encodeURIComponent(outcome)}`
      : `/activities/${id}/complete`
    const response = await apiClient.post<Activity>(endpoint)
    return response.data!
  },

  /**
   * Cancel activity
   */
  cancel: async (id: string, reason?: string): Promise<Activity> => {
    const endpoint = reason 
      ? `/activities/${id}/cancel?reason=${encodeURIComponent(reason)}`
      : `/activities/${id}/cancel`
    const response = await apiClient.post<Activity>(endpoint)
    return response.data!
  },

  /**
   * Reschedule activity
   */
  reschedule: async (id: string, newDate: string): Promise<Activity> => {
    const response = await apiClient.post<Activity>(`/activities/${id}/reschedule`, { scheduledAt: newDate })
    return response.data!
  },

  /**
   * Get activity statistics
   */
  getStats: async (): Promise<ActivityStats> => {
    const response = await apiClient.get<ActivityStats>('/activities/stats')
    return response.data!
  },

  /**
   * Search activities
   */
  search: async (keyword: string): Promise<Activity[]> => {
    const response = await apiClient.get<Activity[]>('/activities/search', { keyword })
    return response.data!
  },
}
