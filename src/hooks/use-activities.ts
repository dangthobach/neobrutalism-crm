/**
 * Activity React Query Hooks
 * Provides data fetching and mutations for activities
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { activityApi } from '@/lib/api/activities'
import type {
  Activity,
  CreateActivityRequest,
  UpdateActivityRequest,
  ActivitySearchParams,
} from '@/types/activity'
import { toast } from 'sonner'

// Query keys
export const activityKeys = {
  all: ['activities'] as const,
  lists: () => [...activityKeys.all, 'list'] as const,
  list: (params?: ActivitySearchParams) => [...activityKeys.lists(), params] as const,
  details: () => [...activityKeys.all, 'detail'] as const,
  detail: (id: string) => [...activityKeys.details(), id] as const,
  byCustomer: (customerId: string) => [...activityKeys.all, 'customer', customerId] as const,
  byContact: (contactId: string) => [...activityKeys.all, 'contact', contactId] as const,
  byAssignee: (userId: string) => [...activityKeys.all, 'assignee', userId] as const,
  today: () => [...activityKeys.all, 'today'] as const,
  thisWeek: () => [...activityKeys.all, 'this-week'] as const,
  upcoming: () => [...activityKeys.all, 'upcoming'] as const,
  overdue: () => [...activityKeys.all, 'overdue'] as const,
  stats: () => [...activityKeys.all, 'stats'] as const,
  search: (keyword: string) => [...activityKeys.all, 'search', keyword] as const,
}

/**
 * Get all activities with pagination
 */
export function useActivities(params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.list(params),
    queryFn: () => activityApi.getAll(params),
  })
}

/**
 * Get activity by ID
 */
export function useActivity(id: string | undefined) {
  return useQuery({
    queryKey: activityKeys.detail(id!),
    queryFn: () => activityApi.getById(id!),
    enabled: !!id,
  })
}

/**
 * Get activities by customer
 */
export function useActivitiesByCustomer(customerId: string, params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.byCustomer(customerId),
    queryFn: () => activityApi.getByCustomer(customerId, params),
    enabled: !!customerId,
  })
}

/**
 * Get activities by contact
 */
export function useActivitiesByContact(contactId: string, params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.byContact(contactId),
    queryFn: () => activityApi.getByContact(contactId, params),
    enabled: !!contactId,
  })
}

/**
 * Get activities assigned to user
 */
export function useActivitiesByAssignee(userId: string, params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.byAssignee(userId),
    queryFn: () => activityApi.getByAssignee(userId, params),
    enabled: !!userId,
  })
}

/**
 * Get today's activities
 */
export function useTodayActivities(params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.today(),
    queryFn: () => activityApi.getToday(params),
  })
}

/**
 * Get this week's activities
 */
export function useThisWeekActivities(params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.thisWeek(),
    queryFn: () => activityApi.getThisWeek(params),
  })
}

/**
 * Get upcoming activities
 */
export function useUpcomingActivities(params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.upcoming(),
    queryFn: () => activityApi.getUpcoming(params),
  })
}

/**
 * Get overdue activities
 */
export function useOverdueActivities(params?: ActivitySearchParams) {
  return useQuery({
    queryKey: activityKeys.overdue(),
    queryFn: () => activityApi.getOverdue(params),
  })
}

/**
 * Get activity statistics
 */
export function useActivityStats() {
  return useQuery({
    queryKey: activityKeys.stats(),
    queryFn: () => activityApi.getStats(),
  })
}

/**
 * Search activities
 */
export function useActivitySearch(keyword: string) {
  return useQuery({
    queryKey: activityKeys.search(keyword),
    queryFn: () => activityApi.search(keyword),
    enabled: keyword.length > 0,
  })
}

/**
 * Create activity
 */
export function useCreateActivity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateActivityRequest) => activityApi.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: activityKeys.lists() })
      queryClient.invalidateQueries({ queryKey: activityKeys.stats() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: activityKeys.byCustomer(data.customerId) })
      }
      if (data.contactId) {
        queryClient.invalidateQueries({ queryKey: activityKeys.byContact(data.contactId) })
      }
      if (data.assignedToId) {
        queryClient.invalidateQueries({ queryKey: activityKeys.byAssignee(data.assignedToId) })
      }
      toast.success('Activity created successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create activity')
    },
  })
}

/**
 * Update activity
 */
export function useUpdateActivity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateActivityRequest }) =>
      activityApi.update(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: activityKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: activityKeys.lists() })
      queryClient.invalidateQueries({ queryKey: activityKeys.stats() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: activityKeys.byCustomer(data.customerId) })
      }
      if (data.contactId) {
        queryClient.invalidateQueries({ queryKey: activityKeys.byContact(data.contactId) })
      }
      toast.success('Activity updated successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update activity')
    },
  })
}

/**
 * Delete activity
 */
export function useDeleteActivity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => activityApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: activityKeys.lists() })
      queryClient.invalidateQueries({ queryKey: activityKeys.stats() })
      queryClient.invalidateQueries({ queryKey: activityKeys.all })
      toast.success('Activity deleted successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete activity')
    },
  })
}

/**
 * Complete activity
 */
export function useCompleteActivity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, outcome }: { id: string; outcome?: string }) =>
      activityApi.complete(id, outcome),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: activityKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: activityKeys.lists() })
      queryClient.invalidateQueries({ queryKey: activityKeys.stats() })
      toast.success('Activity marked as completed')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to complete activity')
    },
  })
}

/**
 * Cancel activity
 */
export function useCancelActivity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      activityApi.cancel(id, reason),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: activityKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: activityKeys.lists() })
      queryClient.invalidateQueries({ queryKey: activityKeys.stats() })
      toast.success('Activity cancelled')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to cancel activity')
    },
  })
}

/**
 * Reschedule activity
 */
export function useRescheduleActivity() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, newDate }: { id: string; newDate: string }) =>
      activityApi.reschedule(id, newDate),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: activityKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: activityKeys.lists() })
      toast.success('Activity rescheduled')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to reschedule activity')
    },
  })
}
