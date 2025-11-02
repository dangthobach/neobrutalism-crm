/**
 * RoleMenu API Service
 * Handles role-menu permission assignments
 */

import { apiClient, ApiResponse } from './client'

export interface RoleMenu {
  id: string
  roleId: string
  menuId: string
  canView: boolean
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  canExport: boolean
  canImport: boolean
  createdAt: string
  createdBy?: string
}

export interface RoleMenuRequest {
  roleId: string
  menuId: string
  canView?: boolean
  canCreate?: boolean
  canEdit?: boolean
  canDelete?: boolean
  canExport?: boolean
  canImport?: boolean
}

export class RoleMenuApi {
  /**
   * Get menu permissions by role ID
   */
  async getMenuPermissionsByRole(roleId: string): Promise<RoleMenu[]> {
    const response = await apiClient.get<RoleMenu[]>(`/role-menus/role/${roleId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get role permissions by menu ID
   */
  async getRolePermissionsByMenu(menuId: string): Promise<RoleMenu[]> {
    const response = await apiClient.get<RoleMenu[]>(`/role-menus/menu/${menuId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Set menu permissions for role
   */
  async setMenuPermissions(request: RoleMenuRequest): Promise<RoleMenu> {
    const response = await apiClient.post<RoleMenu>('/role-menus', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update menu permissions
   */
  async updateMenuPermissions(id: string, request: RoleMenuRequest): Promise<RoleMenu> {
    const response = await apiClient.put<RoleMenu>(`/role-menus/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Revoke menu permission by ID
   */
  async revokeMenuPermission(id: string): Promise<void> {
    await apiClient.delete(`/role-menus/${id}`)
  }

  /**
   * Revoke specific menu permission by role and menu
   */
  async revokeSpecificMenuPermission(roleId: string, menuId: string): Promise<void> {
    await apiClient.delete(`/role-menus/role/${roleId}/menu/${menuId}`)
  }

  /**
   * Copy permissions from another role
   */
  async copyPermissionsFromRole(targetRoleId: string, sourceRoleId: string): Promise<RoleMenu[]> {
    const response = await apiClient.post<RoleMenu[]>(
      `/role-menus/role/${targetRoleId}/copy-from/${sourceRoleId}`
    )
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Bulk update menu permissions for a role
   */
  async bulkUpdateMenuPermissions(roleId: string, permissions: RoleMenuRequest[]): Promise<RoleMenu[]> {
    // First, get existing permissions
    const existing = await this.getMenuPermissionsByRole(roleId)
    const existingMap = new Map(existing.map(p => [p.menuId, p]))

    const results: RoleMenu[] = []

    for (const perm of permissions) {
      const existingPerm = existingMap.get(perm.menuId)

      if (existingPerm) {
        // Update existing
        const updated = await this.updateMenuPermissions(existingPerm.id, perm)
        results.push(updated)
      } else {
        // Create new
        const created = await this.setMenuPermissions(perm)
        results.push(created)
      }
    }

    return results
  }
}

// Export singleton instance
export const roleMenuApi = new RoleMenuApi()
