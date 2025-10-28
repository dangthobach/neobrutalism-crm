/**
 * React Query hooks for RoleMenu (permission) management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { roleMenuApi, RoleMenu, RoleMenuRequest } from '@/lib/api/role-menus'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const ROLE_MENUS_QUERY_KEY = 'role-menus'

/**
 * Fetch menu permissions by role ID
 */
export function useMenuPermissionsByRole(roleId: string) {
  return useQuery({
    queryKey: [ROLE_MENUS_QUERY_KEY, 'role', roleId],
    queryFn: () => roleMenuApi.getMenuPermissionsByRole(roleId),
    enabled: !!roleId,
  })
}

/**
 * Fetch role permissions by menu ID
 */
export function useRolePermissionsByMenu(menuId: string) {
  return useQuery({
    queryKey: [ROLE_MENUS_QUERY_KEY, 'menu', menuId],
    queryFn: () => roleMenuApi.getRolePermissionsByMenu(menuId),
    enabled: !!menuId,
  })
}

/**
 * Set menu permissions for role
 */
export function useSetMenuPermissions() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: RoleMenuRequest) => roleMenuApi.setMenuPermissions(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ROLE_MENUS_QUERY_KEY] })
      toast.success('Menu permissions set successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to set menu permissions', {
        description: error.message,
      })
    },
  })
}

/**
 * Update menu permissions
 */
export function useUpdateMenuPermissions() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RoleMenuRequest }) =>
      roleMenuApi.updateMenuPermissions(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ROLE_MENUS_QUERY_KEY] })
      toast.success('Menu permissions updated successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update menu permissions', {
        description: error.message,
      })
    },
  })
}

/**
 * Revoke menu permission
 */
export function useRevokeMenuPermission() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => roleMenuApi.revokeMenuPermission(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ROLE_MENUS_QUERY_KEY] })
      toast.success('Menu permission revoked successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to revoke menu permission', {
        description: error.message,
      })
    },
  })
}

/**
 * Copy permissions from another role
 */
export function useCopyPermissionsFromRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ targetRoleId, sourceRoleId }: { targetRoleId: string; sourceRoleId: string }) =>
      roleMenuApi.copyPermissionsFromRole(targetRoleId, sourceRoleId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ROLE_MENUS_QUERY_KEY] })
      toast.success(`Successfully copied ${data.length} permissions`)
    },
    onError: (error: ApiError) => {
      toast.error('Failed to copy permissions', {
        description: error.message,
      })
    },
  })
}

/**
 * Bulk update menu permissions for a role
 */
export function useBulkUpdateMenuPermissions() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ roleId, permissions }: { roleId: string; permissions: RoleMenuRequest[] }) =>
      roleMenuApi.bulkUpdateMenuPermissions(roleId, permissions),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ROLE_MENUS_QUERY_KEY] })
      toast.success(`Successfully updated ${data.length} permissions`)
    },
    onError: (error: ApiError) => {
      toast.error('Failed to bulk update permissions', {
        description: error.message,
      })
    },
  })
}
