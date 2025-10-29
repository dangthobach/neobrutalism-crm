/**
 * Menu Tab API Service
 * Handles all menu tab-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export interface MenuTab {
  id: string
  code: string
  name: string
  menuId: string
  icon?: string
  displayOrder: number
  isVisible: boolean
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface MenuTabQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
  menuId?: string
  isVisible?: boolean
}

export interface CreateMenuTabRequest {
  code: string
  name: string
  menuId: string
  icon?: string
  displayOrder?: number
  isVisible?: boolean
}

export interface UpdateMenuTabRequest {
  code?: string
  name?: string
  menuId?: string
  icon?: string
  displayOrder?: number
  isVisible?: boolean
}

export class MenuTabApi {
  /**
   * Get all menu tabs with pagination
   */
  async getMenuTabs(params?: MenuTabQueryParams): Promise<PageResponse<MenuTab>> {
    const response = await apiClient.get<PageResponse<MenuTab>>('/menu-tabs', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get menu tab by ID
   */
  async getMenuTabById(id: string): Promise<MenuTab> {
    const response = await apiClient.get<MenuTab>(`/menu-tabs/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get menu tab by code
   */
  async getMenuTabByCode(code: string): Promise<MenuTab> {
    const response = await apiClient.get<MenuTab>(`/menu-tabs/code/${code}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get tabs by menu ID
   */
  async getTabsByMenu(menuId: string): Promise<MenuTab[]> {
    const response = await apiClient.get<MenuTab[]>(`/menu-tabs/menu/${menuId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get visible tabs by menu ID
   */
  async getVisibleTabsByMenu(menuId: string): Promise<MenuTab[]> {
    const response = await apiClient.get<MenuTab[]>(`/menu-tabs/menu/${menuId}/visible`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new menu tab
   */
  async createMenuTab(request: CreateMenuTabRequest): Promise<MenuTab> {
    const response = await apiClient.post<MenuTab>('/menu-tabs', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update menu tab
   */
  async updateMenuTab(id: string, request: UpdateMenuTabRequest): Promise<MenuTab> {
    const response = await apiClient.put<MenuTab>(`/menu-tabs/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Delete menu tab (soft delete)
   */
  async deleteMenuTab(id: string): Promise<void> {
    await apiClient.delete(`/menu-tabs/${id}`)
  }

  /**
   * Check code availability
   */
  async checkCode(code: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/menu-tabs/check-code/${code}`)
    return response.data ?? false
  }

  /**
   * Reorder tabs within a menu
   */
  async reorderTabs(menuId: string, tabIds: string[]): Promise<void> {
    await apiClient.post(`/menu-tabs/menu/${menuId}/reorder`, { tabIds })
  }
}

// Export singleton instance
export const menuTabApi = new MenuTabApi()
