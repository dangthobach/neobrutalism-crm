/**
 * React Query hooks for GroupRole management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { groupRoleApi, GroupRole, GroupRoleRequest } from '@/lib/api/group-roles'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const GROUP_ROLES_QUERY_KEY = 'group-roles'

/**
 * Fetch roles by group ID
 */
export function useRolesByGroup(groupId: string) {
  return useQuery({
    queryKey: [GROUP_ROLES_QUERY_KEY, 'group', groupId],
    queryFn: () => groupRoleApi.getRolesByGroup(groupId),
    enabled: !!groupId,
  })
}

/**
 * Fetch groups by role ID
 */
export function useGroupsByRole(roleId: string) {
  return useQuery({
    queryKey: [GROUP_ROLES_QUERY_KEY, 'role', roleId],
    queryFn: () => groupRoleApi.getGroupsByRole(roleId),
    enabled: !!roleId,
  })
}

/**
 * Assign role to group
 */
export function useAssignRoleToGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: GroupRoleRequest) => groupRoleApi.assignRoleToGroup(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [GROUP_ROLES_QUERY_KEY] })
      toast.success('Role assigned to group successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to assign role to group', {
        description: error.message,
      })
    },
  })
}

/**
 * Revoke role from group
 */
export function useRevokeRoleFromGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => groupRoleApi.revokeRoleFromGroup(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [GROUP_ROLES_QUERY_KEY] })
      toast.success('Role revoked from group successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to revoke role from group', {
        description: error.message,
      })
    },
  })
}
