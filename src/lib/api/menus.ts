/**
 * Menu API Service
 * Handles all menu-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export interface Menu {
  id: string
  code: string
  name: string
  icon?: string
  parentId?: string
  level: number
  path: string
  route?: string
  displayOrder: number
  isVisible: boolean
  requiresAuth: boolean
  deleted: boolean
  createdAt: string
}

export interface MenuPermissions {
  canView: boolean
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  canExport: boolean
  canImport: boolean
}

export interface UserMenu {
  id: string
  code: string
  name: string
  icon?: string
  route?: string
  displayOrder: number
  parentId?: string
  level: number
  isVisible: boolean
  requiresAuth: boolean
  permissions: MenuPermissions
  children: UserMenu[]
}

export interface MenuQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export class MenuApi {
  /**
   * Get all menus with pagination
   */
  async getMenus(params?: MenuQueryParams): Promise<PageResponse<Menu>> {
    const response = await apiClient.get<PageResponse<Menu>>('/menus', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get menu by ID
   */
  async getMenuById(id: string): Promise<Menu> {
    const response = await apiClient.get<Menu>(`/menus/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get menu by code
   */
  async getMenuByCode(code: string): Promise<Menu> {
    const response = await apiClient.get<Menu>(`/menus/code/${code}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get child menus
   */
  async getChildMenus(parentId: string): Promise<Menu[]> {
    const response = await apiClient.get<Menu[]>(`/menus/parent/${parentId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get root menus
   */
  async getRootMenus(): Promise<Menu[]> {
    const response = await apiClient.get<Menu[]>('/menus/root')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get visible menus
   */
  async getVisibleMenus(): Promise<Menu[]> {
    const response = await apiClient.get<Menu[]>('/menus/visible')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get current user's menu tree with permissions
   */
  async getCurrentUserMenus(): Promise<UserMenu[]> {
    const response = await apiClient.get<UserMenu[]>('/users/me/menus')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get specific user's menu tree with permissions
   */
  async getUserMenus(userId: string): Promise<UserMenu[]> {
    const response = await apiClient.get<UserMenu[]>(`/users/${userId}/menus`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new menu
   */
  async createMenu(menu: Partial<Menu>): Promise<Menu> {
    const response = await apiClient.post<Menu>('/menus', menu)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update menu
   */
  async updateMenu(id: string, menu: Partial<Menu>): Promise<Menu> {
    const response = await apiClient.put<Menu>(`/menus/${id}`, menu)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Delete menu (soft delete)
   */
  async deleteMenu(id: string): Promise<void> {
    await apiClient.delete(`/menus/${id}`)
  }

  /**
   * Check code availability
   */
  async checkCode(code: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/menus/check-code/${code}`)
    return response.data ?? false
  }
}

// Export singleton instance
export const menuApi = new MenuApi()
