/**
 * Permission API Service
 * Handles permission checks and user menu retrieval
 */

import { apiClient, ApiResponse } from './client'

export interface MenuPermissions {
  canView?: boolean
  canCreate?: boolean
  canEdit?: boolean
  canDelete?: boolean
  canExport?: boolean
  canImport?: boolean
}

export interface UserMenu {
  id: string
  code: string
  name: string
  route?: string
  icon?: string
  displayOrder: number
  permissions?: MenuPermissions
  children?: UserMenu[]
}

export interface UserMenuResponse {
  menus: UserMenu[]
}

export class PermissionApi {
  /**
   * Get current user's menus with permissions
   */
  async getUserMenus(): Promise<UserMenu[]> {
    const response = await apiClient.get<UserMenu[]>('/users/me/menus')
    return response ?? []
  }

  /**
   * Get current user's permissions for a specific route/code
   */
  async getPermissions(routeOrCode: string): Promise<MenuPermissions> {
    const response = await apiClient.get<MenuPermissions>(
      `/users/me/permissions?route=${encodeURIComponent(routeOrCode)}`
    )
    return response ?? {}
  }

  /**
   * Check if user has specific permission
   */
  async checkPermission(routeOrCode: string, action: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(
      `/users/me/permissions/check?route=${encodeURIComponent(routeOrCode)}&action=${action}`
    )
    return response ?? false
  }
}

export const permissionApi = new PermissionApi()
