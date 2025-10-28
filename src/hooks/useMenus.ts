/**
 * React Query hooks for Menu management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { menuApi, Menu, MenuQueryParams, UserMenu } from '@/lib/api/menus'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const MENUS_QUERY_KEY = 'menus'

/**
 * Fetch menus with pagination
 */
export function useMenus(params?: MenuQueryParams) {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, params],
    queryFn: () => menuApi.getMenus(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch menu by ID
 */
export function useMenu(id: string) {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, id],
    queryFn: () => menuApi.getMenuById(id),
    enabled: !!id,
  })
}

/**
 * Fetch menu by code
 */
export function useMenuByCode(code: string) {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, 'code', code],
    queryFn: () => menuApi.getMenuByCode(code),
    enabled: !!code,
  })
}

/**
 * Fetch child menus
 */
export function useChildMenus(parentId: string) {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, 'parent', parentId],
    queryFn: () => menuApi.getChildMenus(parentId),
    enabled: !!parentId,
  })
}

/**
 * Fetch root menus
 */
export function useRootMenus() {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, 'root'],
    queryFn: () => menuApi.getRootMenus(),
  })
}

/**
 * Fetch visible menus
 */
export function useVisibleMenus() {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, 'visible'],
    queryFn: () => menuApi.getVisibleMenus(),
  })
}

/**
 * Fetch current user's menu tree with permissions
 */
export function useCurrentUserMenus() {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, 'current-user'],
    queryFn: () => menuApi.getCurrentUserMenus(),
  })
}

/**
 * Fetch specific user's menu tree with permissions
 */
export function useUserMenus(userId: string) {
  return useQuery({
    queryKey: [MENUS_QUERY_KEY, 'user', userId],
    queryFn: () => menuApi.getUserMenus(userId),
    enabled: !!userId,
  })
}

/**
 * Create new menu
 */
export function useCreateMenu() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: Partial<Menu>) => menuApi.createMenu(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [MENUS_QUERY_KEY] })
      toast.success('Menu created successfully', {
        description: `${data.name} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create menu', {
        description: error.message,
      })
    },
  })
}

/**
 * Update menu
 */
export function useUpdateMenu() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Menu> }) =>
      menuApi.updateMenu(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [MENUS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [MENUS_QUERY_KEY, variables.id] })
      toast.success('Menu updated successfully', {
        description: `${data.name} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update menu', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete menu
 */
export function useDeleteMenu() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => menuApi.deleteMenu(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MENUS_QUERY_KEY] })
      toast.success('Menu deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete menu', {
        description: error.message,
      })
    },
  })
}

/**
 * Check code availability
 */
export function useCheckMenuCode() {
  return useMutation({
    mutationFn: (code: string) => menuApi.checkCode(code),
  })
}
