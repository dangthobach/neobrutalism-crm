/**
 * React Query hooks for Menu Screen management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  menuScreenApi,
  MenuScreen,
  MenuScreenQueryParams,
  CreateMenuScreenRequest,
  UpdateMenuScreenRequest,
  ScreenApiAssignment
} from '@/lib/api/menu-screens'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const MENU_SCREENS_QUERY_KEY = 'menu-screens'
const SCREEN_API_ENDPOINTS_QUERY_KEY = 'screen-api-endpoints'

/**
 * Fetch menu screens with pagination
 */
export function useMenuScreens(params?: MenuScreenQueryParams) {
  return useQuery({
    queryKey: [MENU_SCREENS_QUERY_KEY, params],
    queryFn: () => menuScreenApi.getMenuScreens(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch menu screen by ID
 */
export function useMenuScreen(id: string) {
  return useQuery({
    queryKey: [MENU_SCREENS_QUERY_KEY, id],
    queryFn: () => menuScreenApi.getMenuScreenById(id),
    enabled: !!id,
  })
}

/**
 * Fetch menu screen by code
 */
export function useMenuScreenByCode(code: string) {
  return useQuery({
    queryKey: [MENU_SCREENS_QUERY_KEY, 'code', code],
    queryFn: () => menuScreenApi.getMenuScreenByCode(code),
    enabled: !!code,
  })
}

/**
 * Fetch screens by menu ID
 */
export function useScreensByMenu(menuId: string) {
  return useQuery({
    queryKey: [MENU_SCREENS_QUERY_KEY, 'menu', menuId],
    queryFn: () => menuScreenApi.getScreensByMenu(menuId),
    enabled: !!menuId,
  })
}

/**
 * Fetch screens by tab ID
 */
export function useScreensByTab(tabId: string) {
  return useQuery({
    queryKey: [MENU_SCREENS_QUERY_KEY, 'tab', tabId],
    queryFn: () => menuScreenApi.getScreensByTab(tabId),
    enabled: !!tabId,
  })
}

/**
 * Fetch API endpoints assigned to a screen
 */
export function useScreenApiEndpoints(screenId: string) {
  return useQuery({
    queryKey: [SCREEN_API_ENDPOINTS_QUERY_KEY, screenId],
    queryFn: () => menuScreenApi.getScreenApiEndpoints(screenId),
    enabled: !!screenId,
  })
}

/**
 * Create new menu screen
 */
export function useCreateMenuScreen() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateMenuScreenRequest) => menuScreenApi.createMenuScreen(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY] })
      if (data.menuId) {
        queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY, 'menu', data.menuId] })
      }
      if (data.tabId) {
        queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY, 'tab', data.tabId] })
      }
      toast.success('Menu screen created successfully', {
        description: `${data.name} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create menu screen', {
        description: error.message,
      })
    },
  })
}

/**
 * Update menu screen
 */
export function useUpdateMenuScreen() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateMenuScreenRequest }) =>
      menuScreenApi.updateMenuScreen(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY, variables.id] })
      if (data.menuId) {
        queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY, 'menu', data.menuId] })
      }
      if (data.tabId) {
        queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY, 'tab', data.tabId] })
      }
      toast.success('Menu screen updated successfully', {
        description: `${data.name} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update menu screen', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete menu screen
 */
export function useDeleteMenuScreen() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => menuScreenApi.deleteMenuScreen(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MENU_SCREENS_QUERY_KEY] })
      toast.success('Menu screen deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete menu screen', {
        description: error.message,
      })
    },
  })
}

/**
 * Check code availability
 */
export function useCheckMenuScreenCode() {
  return useMutation({
    mutationFn: (code: string) => menuScreenApi.checkCode(code),
  })
}

/**
 * Assign API endpoints to a screen
 */
export function useAssignApiEndpoints() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ screenId, apiEndpointIds }: { screenId: string; apiEndpointIds: string[] }) =>
      menuScreenApi.assignApiEndpoints(screenId, apiEndpointIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [SCREEN_API_ENDPOINTS_QUERY_KEY, variables.screenId] })
      toast.success('API endpoints assigned successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to assign API endpoints', {
        description: error.message,
      })
    },
  })
}

/**
 * Remove API endpoint from screen
 */
export function useRemoveApiEndpoint() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ screenId, apiEndpointId }: { screenId: string; apiEndpointId: string }) =>
      menuScreenApi.removeApiEndpoint(screenId, apiEndpointId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [SCREEN_API_ENDPOINTS_QUERY_KEY, variables.screenId] })
      toast.success('API endpoint removed successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to remove API endpoint', {
        description: error.message,
      })
    },
  })
}

/**
 * Bulk assign screens to API endpoints
 */
export function useBulkAssignScreenApis() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (assignments: ScreenApiAssignment[]) =>
      menuScreenApi.bulkAssignScreenApis(assignments),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [SCREEN_API_ENDPOINTS_QUERY_KEY] })
      toast.success('API endpoints assigned in bulk successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to bulk assign API endpoints', {
        description: error.message,
      })
    },
  })
}
