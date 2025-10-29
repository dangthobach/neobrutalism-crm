/**
 * React Query hooks for Group management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { groupApi, Group, GroupQueryParams, GroupStatus } from '@/lib/api/groups'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const GROUPS_QUERY_KEY = 'groups'

/**
 * Fetch groups with pagination
 */
export function useGroups(params?: GroupQueryParams) {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, params],
    queryFn: () => groupApi.getGroups(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch group by ID
 */
export function useGroup(id: string) {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, id],
    queryFn: () => groupApi.getGroupById(id),
    enabled: !!id,
  })
}

/**
 * Fetch group by code
 */
export function useGroupByCode(code: string) {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, 'code', code],
    queryFn: () => groupApi.getGroupByCode(code),
    enabled: !!code,
  })
}

/**
 * Fetch groups by organization
 */
export function useGroupsByOrganization(organizationId: string) {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, 'organization', organizationId],
    queryFn: () => groupApi.getGroupsByOrganization(organizationId),
    enabled: !!organizationId,
  })
}

/**
 * Fetch child groups
 */
export function useChildGroups(parentId: string) {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, 'parent', parentId],
    queryFn: () => groupApi.getChildGroups(parentId),
    enabled: !!parentId,
  })
}

/**
 * Fetch root groups
 */
export function useRootGroups() {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, 'root'],
    queryFn: () => groupApi.getRootGroups(),
  })
}

/**
 * Fetch groups by status
 */
export function useGroupsByStatus(status: GroupStatus) {
  return useQuery({
    queryKey: [GROUPS_QUERY_KEY, 'status', status],
    queryFn: () => groupApi.getGroupsByStatus(status),
  })
}

/**
 * Create new group
 */
export function useCreateGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: Partial<Group>) => groupApi.createGroup(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [GROUPS_QUERY_KEY] })
      toast.success('Group created successfully', {
        description: `${data.name} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create group', {
        description: error.message,
      })
    },
  })
}

/**
 * Update group
 */
export function useUpdateGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Group> }) =>
      groupApi.updateGroup(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [GROUPS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [GROUPS_QUERY_KEY, variables.id] })
      toast.success('Group updated successfully', {
        description: `${data.name} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update group', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete group
 */
export function useDeleteGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => groupApi.deleteGroup(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [GROUPS_QUERY_KEY] })
      toast.success('Group deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete group', {
        description: error.message,
      })
    },
  })
}

/**
 * Activate group
 */
export function useActivateGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => groupApi.activateGroup(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [GROUPS_QUERY_KEY] })
      toast.success('Group activated', {
        description: `${data.name} is now active.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to activate group', {
        description: error.message,
      })
    },
  })
}

/**
 * Suspend group
 */
export function useSuspendGroup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => groupApi.suspendGroup(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [GROUPS_QUERY_KEY] })
      toast.success('Group suspended', {
        description: `${data.name} has been suspended.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to suspend group', {
        description: error.message,
      })
    },
  })
}

/**
 * Check code availability
 */
export function useCheckGroupCode() {
  return useMutation({
    mutationFn: (code: string) => groupApi.checkCode(code),
  })
}
