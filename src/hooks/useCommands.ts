/**
 * React Query hooks for Command Palette management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  commandApi,
  Command,
  CommandSearchRequest,
  CommandExecutionRequest,
  CommandCategory,
} from '@/lib/api/commands'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const COMMANDS_QUERY_KEY = 'commands'

/**
 * Search commands
 */
export function useSearchCommands(params?: CommandSearchRequest) {
  return useQuery({
    queryKey: [COMMANDS_QUERY_KEY, 'search', params],
    queryFn: () => commandApi.searchCommands(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Get recent commands for current user
 */
export function useRecentCommands() {
  return useQuery({
    queryKey: [COMMANDS_QUERY_KEY, 'recent'],
    queryFn: () => commandApi.getRecentCommands(),
    staleTime: 1 * 60 * 1000, // 1 minute
  })
}

/**
 * Get favorite commands for current user
 */
export function useFavoriteCommands() {
  return useQuery({
    queryKey: [COMMANDS_QUERY_KEY, 'favorites'],
    queryFn: () => commandApi.getFavoriteCommands(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Get suggested commands for current user
 */
export function useSuggestedCommands(limit = 10) {
  return useQuery({
    queryKey: [COMMANDS_QUERY_KEY, 'suggestions', limit],
    queryFn: () => commandApi.getSuggestedCommands(limit),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Add command to favorites mutation
 */
export function useAddToFavorites() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (commandId: string) => commandApi.addToFavorites(commandId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COMMANDS_QUERY_KEY, 'favorites'] })
      toast.success('Command added to favorites')
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to add command to favorites')
    },
  })
}

/**
 * Remove command from favorites mutation
 */
export function useRemoveFromFavorites() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (commandId: string) => commandApi.removeFromFavorites(commandId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COMMANDS_QUERY_KEY, 'favorites'] })
      toast.success('Command removed from favorites')
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to remove command from favorites')
    },
  })
}

/**
 * Record command execution mutation
 */
export function useRecordExecution() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CommandExecutionRequest) => commandApi.recordExecution(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COMMANDS_QUERY_KEY, 'recent'] })
      queryClient.invalidateQueries({ queryKey: [COMMANDS_QUERY_KEY, 'suggestions'] })
    },
    onError: (error: ApiError) => {
      // Silent fail - don't show toast for execution tracking failures
      console.error('Failed to record command execution:', error)
    },
  })
}
