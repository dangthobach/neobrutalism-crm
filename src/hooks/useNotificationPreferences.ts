/**
 * Notification Preferences React Query Hooks
 * For managing user notification preferences with caching and optimistic updates
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { 
  notificationPreferenceApi, 
  type NotificationPreference,
  type UpdatePreferenceRequest,
  type BatchUpdatePreferencesRequest 
} from '@/lib/api/notification-preferences'
import { toast } from 'sonner'

// Query keys
export const preferenceKeys = {
  all: ['notification-preferences'] as const,
  my: () => [...preferenceKeys.all, 'my'] as const,
  type: (type: string) => [...preferenceKeys.all, 'type', type] as const,
}

/**
 * Get all notification preferences for current user
 */
export function useNotificationPreferences() {
  return useQuery({
    queryKey: preferenceKeys.my(),
    queryFn: () => notificationPreferenceApi.getMyPreferences(),
    staleTime: 60000, // 1 minute - preferences don't change often
    gcTime: 300000, // 5 minutes cache
  })
}

/**
 * Get preference for specific notification type
 */
export function useNotificationPreference(type: string) {
  return useQuery({
    queryKey: preferenceKeys.type(type),
    queryFn: () => notificationPreferenceApi.getPreferenceByType(type),
    enabled: !!type,
    staleTime: 60000,
  })
}

/**
 * Update notification preference with optimistic updates
 */
export function useUpdateNotificationPreference() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ type, data }: { type: string; data: UpdatePreferenceRequest }) =>
      notificationPreferenceApi.updatePreference(type, data),
    
    onMutate: async ({ type, data }) => {
      // Cancel outgoing queries
      await queryClient.cancelQueries({ queryKey: preferenceKeys.my() })
      
      // Get previous data
      const previousPreferences = queryClient.getQueryData<NotificationPreference[]>(preferenceKeys.my())
      
      // Optimistically update
      if (previousPreferences) {
        queryClient.setQueryData<NotificationPreference[]>(
          preferenceKeys.my(),
          previousPreferences.map(p =>
            p.notificationType === type
              ? { ...p, ...data }
              : p
          )
        )
      }
      
      return { previousPreferences }
    },
    
    onError: (error: any, _, context) => {
      // Rollback on error
      if (context?.previousPreferences) {
        queryClient.setQueryData(preferenceKeys.my(), context.previousPreferences)
      }
      toast.error('Failed to update preference')
    },
    
    onSuccess: (data, { type }) => {
      // Update individual preference cache
      queryClient.setQueryData(preferenceKeys.type(type), data)
      toast.success('Preference updated')
    },
  })
}

/**
 * Batch update multiple preferences
 */
export function useBatchUpdatePreferences() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: BatchUpdatePreferencesRequest) =>
      notificationPreferenceApi.batchUpdatePreferences(data),
    
    onMutate: async (data) => {
      await queryClient.cancelQueries({ queryKey: preferenceKeys.my() })
      const previousPreferences = queryClient.getQueryData<NotificationPreference[]>(preferenceKeys.my())
      
      // Optimistically update all
      if (previousPreferences) {
        const updateMap = new Map(data.preferences.map(p => [p.notificationType, p]))
        queryClient.setQueryData<NotificationPreference[]>(
          preferenceKeys.my(),
          previousPreferences.map(p => {
            const update = updateMap.get(p.notificationType)
            return update ? { ...p, ...update } : p
          })
        )
      }
      
      return { previousPreferences }
    },
    
    onError: (error: any, _, context) => {
      if (context?.previousPreferences) {
        queryClient.setQueryData(preferenceKeys.my(), context.previousPreferences)
      }
      toast.error('Failed to update preferences')
    },
    
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: preferenceKeys.all })
      toast.success('Preferences updated successfully')
    },
  })
}

/**
 * Reset preferences to defaults
 */
export function useResetPreferences() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationPreferenceApi.resetToDefaults(),
    onSuccess: (data) => {
      queryClient.setQueryData(preferenceKeys.my(), data)
      toast.success('Preferences reset to defaults')
    },
    onError: () => {
      toast.error('Failed to reset preferences')
    },
  })
}

/**
 * Enable all notification channels
 */
export function useEnableAllPreferences() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationPreferenceApi.enableAll(),
    onSuccess: (data) => {
      queryClient.setQueryData(preferenceKeys.my(), data)
      toast.success('All notifications enabled')
    },
    onError: () => {
      toast.error('Failed to enable all notifications')
    },
  })
}

/**
 * Disable all notification channels
 */
export function useDisableAllPreferences() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationPreferenceApi.disableAll(),
    onSuccess: (data) => {
      queryClient.setQueryData(preferenceKeys.my(), data)
      toast.success('All notifications disabled')
    },
    onError: () => {
      toast.error('Failed to disable all notifications')
    },
  })
}
