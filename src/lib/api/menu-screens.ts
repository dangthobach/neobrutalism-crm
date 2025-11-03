/**
 * Menu Screen API Service
 * Handles all menu screen-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export interface MenuScreen {
  id: string
  code: string
  name: string
  menuId?: string
  tabId?: string
  route?: string
  component?: string
  requiresPermission: boolean
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface MenuScreenQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
  menuId?: string
  tabId?: string
  requiresPermission?: boolean
}

export interface CreateMenuScreenRequest {
  code: string
  name: string
  menuId?: string
  tabId?: string
  route?: string
  component?: string
  requiresPermission?: boolean
}

export interface UpdateMenuScreenRequest {
  code?: string
  name?: string
  menuId?: string
  tabId?: string
  route?: string
  component?: string
  requiresPermission?: boolean
}

export interface ScreenApiAssignment {
  screenId: string
  apiEndpointIds: string[]
}

export class MenuScreenApi {
  /**
   * Get all menu screens with pagination
   */
  async getMenuScreens(params?: MenuScreenQueryParams): Promise<PageResponse<MenuScreen>> {
    return apiClient.get<PageResponse<MenuScreen>>('/menu-screens', params)
  }

  /**
   * Get menu screen by ID
   */
  async getMenuScreenById(id: string): Promise<MenuScreen> {
    return apiClient.get<MenuScreen>(`/menu-screens/${id}`)
  }

  /**
   * Get menu screen by code
   */
  async getMenuScreenByCode(code: string): Promise<MenuScreen> {
    return apiClient.get<MenuScreen>(`/menu-screens/code/${code}`)
  }

  /**
   * Get screens by menu ID
   */
  async getScreensByMenu(menuId: string): Promise<MenuScreen[]> {
    return apiClient.get<MenuScreen[]>(`/menu-screens/menu/${menuId}`)
  }

  /**
   * Get screens by tab ID
   */
  async getScreensByTab(tabId: string): Promise<MenuScreen[]> {
    return apiClient.get<MenuScreen[]>(`/menu-screens/tab/${tabId}`)
  }

  /**
   * Create new menu screen
   */
  async createMenuScreen(request: CreateMenuScreenRequest): Promise<MenuScreen> {
    return apiClient.post<MenuScreen>('/menu-screens', request)
  }

  /**
   * Update menu screen
   */
  async updateMenuScreen(id: string, request: UpdateMenuScreenRequest): Promise<MenuScreen> {
    return apiClient.put<MenuScreen>(`/menu-screens/${id}`, request)
  }

  /**
   * Delete menu screen (soft delete)
   */
  async deleteMenuScreen(id: string): Promise<void> {
    await apiClient.delete(`/menu-screens/${id}`)
  }

  /**
   * Check code availability
   */
  async checkCode(code: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/menu-screens/check-code/${code}`)
    return response ?? false
  }

  /**
   * Get API endpoints assigned to a screen
   */
  async getScreenApiEndpoints(screenId: string): Promise<string[]> {
    return apiClient.get<string[]>(`/menu-screens/${screenId}/api-endpoints`)
  }

  /**
   * Assign API endpoints to a screen
   */
  async assignApiEndpoints(screenId: string, apiEndpointIds: string[]): Promise<void> {
    await apiClient.post(`/menu-screens/${screenId}/api-endpoints`, { apiEndpointIds })
  }

  /**
   * Remove API endpoint from screen
   */
  async removeApiEndpoint(screenId: string, apiEndpointId: string): Promise<void> {
    await apiClient.delete(`/menu-screens/${screenId}/api-endpoints/${apiEndpointId}`)
  }

  /**
   * Bulk assign screens to API endpoints
   */
  async bulkAssignScreenApis(assignments: ScreenApiAssignment[]): Promise<void> {
    await apiClient.post('/menu-screens/bulk-assign-apis', { assignments })
  }
}

// Export singleton instance
export const menuScreenApi = new MenuScreenApi()
