/**
 * React Query hooks for UserGroup management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userGroupApi, UserGroup, UserGroupRequest } from '@/lib/api/user-groups'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const USER_GROUPS_QUERY_KEY = 'user-groups'

/**
 * Fetch groups by user ID
 */
export function useGroupsByUser(userId: string) {
  return useQuery({
    queryKey: [USER_GROUPS_QUERY_KEY, 'user', userId],
    queryFn: () => userGroupApi.getGroupsByUser(userId),
    enabled: !!userId,
  })
}

/**
 * Fetch users by group ID
 */
export function useUsersByGroup(groupId: string) {
  return useQuery({
    queryKey: [USER_GROUPS_QUERY_KEY, 'group', groupId],
    queryFn: () => userGroupApi.getUsersByGroup(groupId),
    enabled: !!groupId,
  })
}

/**
 * Fetch user's primary group
 */
export function usePrimaryGroup(userId: string) {
  return useQuery({
    queryKey: [USER_GROUPS_QUERY_KEY, 'user', userId, 'primary'],
    queryFn: () => userGroupApi.getPrimaryGroup(userId),
    enabled: !!userId,
  })
}

/**
 * Assign user to group
 */
export function useAssignUserToGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: UserGroupRequest) => userGroupApi.assignUserToGroup(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_GROUPS_QUERY_KEY] })
      toast.success('User assigned to group successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to assign user to group', {
        description: error.message,
      })
    },
  })
}

/**
 * Update user-group assignment
 */
export function useUpdateUserGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UserGroupRequest }) =>
      userGroupApi.updateUserGroup(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_GROUPS_QUERY_KEY] })
      toast.success('User-group assignment updated successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update user-group assignment', {
        description: error.message,
      })
    },
  })
}

/**
 * Remove user from group
 */
export function useRemoveUserFromGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => userGroupApi.removeUserFromGroup(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_GROUPS_QUERY_KEY] })
      toast.success('User removed from group successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to remove user from group', {
        description: error.message,
      })
    },
  })
}

/**
 * Set as primary group
 */
export function useSetPrimaryGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => userGroupApi.setPrimaryGroup(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_GROUPS_QUERY_KEY] })
      toast.success('Primary group set successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to set primary group', {
        description: error.message,
      })
    },
  })
}
