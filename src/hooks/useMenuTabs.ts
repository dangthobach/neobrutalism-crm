/**
 * React Query hooks for Menu Tab management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  menuTabApi,
  MenuTab,
  MenuTabQueryParams,
  CreateMenuTabRequest,
  UpdateMenuTabRequest
} from '@/lib/api/menu-tabs'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const MENU_TABS_QUERY_KEY = 'menu-tabs'

/**
 * Fetch menu tabs with pagination
 */
export function useMenuTabs(params?: MenuTabQueryParams) {
  return useQuery({
    queryKey: [MENU_TABS_QUERY_KEY, params],
    queryFn: () => menuTabApi.getMenuTabs(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch menu tab by ID
 */
export function useMenuTab(id: string) {
  return useQuery({
    queryKey: [MENU_TABS_QUERY_KEY, id],
    queryFn: () => menuTabApi.getMenuTabById(id),
    enabled: !!id,
  })
}

/**
 * Fetch menu tab by code
 */
export function useMenuTabByCode(code: string) {
  return useQuery({
    queryKey: [MENU_TABS_QUERY_KEY, 'code', code],
    queryFn: () => menuTabApi.getMenuTabByCode(code),
    enabled: !!code,
  })
}

/**
 * Fetch tabs by menu ID
 */
export function useTabsByMenu(menuId: string) {
  return useQuery({
    queryKey: [MENU_TABS_QUERY_KEY, 'menu', menuId],
    queryFn: () => menuTabApi.getTabsByMenu(menuId),
    enabled: !!menuId,
  })
}

/**
 * Fetch visible tabs by menu ID
 */
export function useVisibleTabsByMenu(menuId: string) {
  return useQuery({
    queryKey: [MENU_TABS_QUERY_KEY, 'menu', menuId, 'visible'],
    queryFn: () => menuTabApi.getVisibleTabsByMenu(menuId),
    enabled: !!menuId,
  })
}

/**
 * Create new menu tab
 */
export function useCreateMenuTab() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateMenuTabRequest) => menuTabApi.createMenuTab(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY, 'menu', data.menuId] })
      toast.success('Menu tab created successfully', {
        description: `${data.name} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create menu tab', {
        description: error.message,
      })
    },
  })
}

/**
 * Update menu tab
 */
export function useUpdateMenuTab() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateMenuTabRequest }) =>
      menuTabApi.updateMenuTab(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY, variables.id] })
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY, 'menu', data.menuId] })
      toast.success('Menu tab updated successfully', {
        description: `${data.name} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update menu tab', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete menu tab
 */
export function useDeleteMenuTab() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => menuTabApi.deleteMenuTab(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY] })
      toast.success('Menu tab deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete menu tab', {
        description: error.message,
      })
    },
  })
}

/**
 * Check code availability
 */
export function useCheckMenuTabCode() {
  return useMutation({
    mutationFn: (code: string) => menuTabApi.checkCode(code),
  })
}

/**
 * Reorder tabs within a menu
 */
export function useReorderMenuTabs() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ menuId, tabIds }: { menuId: string; tabIds: string[] }) =>
      menuTabApi.reorderTabs(menuId, tabIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [MENU_TABS_QUERY_KEY, 'menu', variables.menuId] })
      toast.success('Tab order updated successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to reorder tabs', {
        description: error.message,
      })
    },
  })
}
