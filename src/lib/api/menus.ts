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

export interface CreateMenuRequest {
  code: string
  name: string
  icon?: string
  parentId?: string
  route?: string
  displayOrder: number
  isVisible: boolean
  requiresAuth: boolean
}

export class MenuApi {
  /**
   * Get all menus with pagination
   */
  async getMenus(params?: MenuQueryParams): Promise<PageResponse<Menu>> {
    return apiClient.get<PageResponse<Menu>>('/menus', params)
  }

  /**
   * Get menu by ID
   */
  async getMenuById(id: string): Promise<Menu> {
    return apiClient.get<Menu>(`/menus/${id}`)
  }

  /**
   * Get menu by code
   */
  async getMenuByCode(code: string): Promise<Menu> {
    return apiClient.get<Menu>(`/menus/code/${code}`)
  }

  /**
   * Get child menus
   */
  async getChildMenus(parentId: string): Promise<Menu[]> {
    return apiClient.get<Menu[]>(`/menus/parent/${parentId}`)
  }

  /**
   * Get root menus
   */
  async getRootMenus(): Promise<Menu[]> {
    return apiClient.get<Menu[]>('/menus/root')
  }

  /**
   * Get visible menus
   */
  async getVisibleMenus(): Promise<Menu[]> {
    return apiClient.get<Menu[]>('/menus/visible')
  }

  /**
   * Get current user's menu tree with permissions
   */
  async getCurrentUserMenus(): Promise<UserMenu[]> {
    return apiClient.get<UserMenu[]>('/users/me/menus')
  }

  /**
   * Get specific user's menu tree with permissions
   */
  async getUserMenus(userId: string): Promise<UserMenu[]> {
    return apiClient.get<UserMenu[]>(`/users/${userId}/menus`)
  }

  /**
   * Create new menu
   */
  async createMenu(menu: Partial<Menu>): Promise<Menu> {
    return apiClient.post<Menu>('/menus', menu)
  }

  /**
   * Update menu
   */
  async updateMenu(id: string, menu: Partial<Menu>): Promise<Menu> {
    return apiClient.put<Menu>(`/menus/${id}`, menu)
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
    return response ?? false
  }
}

// Export singleton instance
export const menuApi = new MenuApi()
