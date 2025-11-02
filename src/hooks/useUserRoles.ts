/**
 * React Query hooks for UserRole management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userRoleApi, UserRole, UserRoleRequest } from '@/lib/api/user-roles'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const USER_ROLES_QUERY_KEY = 'user-roles'

/**
 * Fetch roles by user ID
 */
export function useRolesByUser(userId: string) {
  return useQuery({
    queryKey: [USER_ROLES_QUERY_KEY, 'user', userId],
    queryFn: () => userRoleApi.getRolesByUser(userId),
    enabled: !!userId,
  })
}

/**
 * Fetch users by role ID
 */
export function useUsersByRole(roleId: string) {
  return useQuery({
    queryKey: [USER_ROLES_QUERY_KEY, 'role', roleId],
    queryFn: () => userRoleApi.getUsersByRole(roleId),
    enabled: !!roleId,
  })
}

/**
 * Assign role to user
 */
export function useAssignRoleToUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: UserRoleRequest) => userRoleApi.assignRole(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_ROLES_QUERY_KEY] })
      toast.success('Role assigned to user successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to assign role to user', {
        description: error.message,
      })
    },
  })
}

/**
 * Update user-role assignment
 */
export function useUpdateUserRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UserRoleRequest }) =>
      userRoleApi.updateUserRole(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_ROLES_QUERY_KEY] })
      toast.success('User-role assignment updated successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update user-role assignment', {
        description: error.message,
      })
    },
  })
}

/**
 * Revoke role from user
 */
export function useRevokeRoleFromUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => userRoleApi.revokeRole(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USER_ROLES_QUERY_KEY] })
      toast.success('Role revoked from user successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to revoke role from user', {
        description: error.message,
      })
    },
  })
}

/**
 * Copy roles from another user
 */
export function useCopyRolesFromUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ targetUserId, sourceUserId }: { targetUserId: string; sourceUserId: string }) =>
      userRoleApi.copyRolesFromUser(targetUserId, sourceUserId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USER_ROLES_QUERY_KEY] })
      toast.success(`Successfully copied ${data.length} roles`)
    },
    onError: (error: ApiError) => {
      toast.error('Failed to copy roles', {
        description: error.message,
      })
    },
  })
}
