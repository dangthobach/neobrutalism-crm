/**
 * React Query hooks for Roles
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { roleApi, Role, CreateRoleRequest, UpdateRoleRequest, RoleQueryParams } from '@/lib/api/roles'
import { toast } from 'sonner'

// Query keys
export const roleKeys = {
  all: ['roles'] as const,
  lists: () => [...roleKeys.all, 'list'] as const,
  list: (params: RoleQueryParams) => [...roleKeys.lists(), params] as const,
  details: () => [...roleKeys.all, 'detail'] as const,
  detail: (id: string) => [...roleKeys.details(), id] as const,
  byCode: (code: string) => [...roleKeys.all, 'code', code] as const,
  byOrganization: (organizationId: string) => [...roleKeys.all, 'organization', organizationId] as const,
}

/**
 * Get roles with pagination
 */
export function useRoles(params: RoleQueryParams = {}) {
  return useQuery({
    queryKey: roleKeys.list(params),
    queryFn: () => roleApi.getRoles(params),
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

/**
 * Get role by ID
 */
export function useRole(id: string | undefined) {
  return useQuery({
    queryKey: roleKeys.detail(id || ''),
    queryFn: () => roleApi.getRoleById(id!),
    enabled: !!id,
  })
}

/**
 * Get role by code
 */
export function useRoleByCode(code: string | undefined) {
  return useQuery({
    queryKey: roleKeys.byCode(code || ''),
    queryFn: () => roleApi.getRoleByCode(code!),
    enabled: !!code,
  })
}

/**
 * Get roles by organization
 */
export function useRolesByOrganization(organizationId: string | undefined) {
  return useQuery({
    queryKey: roleKeys.byOrganization(organizationId || ''),
    queryFn: () => roleApi.getRolesByOrganization(organizationId!),
    enabled: !!organizationId,
  })
}

/**
 * Create a new role
 */
export function useCreateRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateRoleRequest) => roleApi.createRole(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: roleKeys.lists() })
      toast.success('Role created successfully')
    },
    onError: (error: Error) => {
      toast.error('Failed to create role', {
        description: error.message,
      })
    },
  })
}

/**
 * Update a role
 */
export function useUpdateRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateRoleRequest }) =>
      roleApi.updateRole(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: roleKeys.lists() })
      queryClient.invalidateQueries({ queryKey: roleKeys.detail(variables.id) })
      toast.success('Role updated successfully')
    },
    onError: (error: Error) => {
      toast.error('Failed to update role', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete a role
 */
export function useDeleteRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => roleApi.deleteRole(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: roleKeys.lists() })
      toast.success('Role deleted successfully')
    },
    onError: (error: Error) => {
      toast.error('Failed to delete role', {
        description: error.message,
      })
    },
  })
}

/**
 * Activate a role
 */
export function useActivateRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => roleApi.activateRole(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: roleKeys.lists() })
      queryClient.invalidateQueries({ queryKey: roleKeys.detail(id) })
      toast.success('Role activated successfully')
    },
    onError: (error: Error) => {
      toast.error('Failed to activate role', {
        description: error.message,
      })
    },
  })
}

/**
 * Suspend a role
 */
export function useSuspendRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => roleApi.suspendRole(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: roleKeys.lists() })
      queryClient.invalidateQueries({ queryKey: roleKeys.detail(id) })
      toast.success('Role suspended successfully')
    },
    onError: (error: Error) => {
      toast.error('Failed to suspend role', {
        description: error.message,
      })
    },
  })
}
