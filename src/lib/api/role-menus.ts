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

export interface ValidationResult {
  valid: boolean
  errors: string[]
  warnings: string[]
  info: string[]
  suggestions: string[]
  errorCount: number
  warningCount: number
  infoCount: number
}

export class RoleMenuApi {
  /**
   * Get menu permissions by role ID
   */
  async getMenuPermissionsByRole(roleId: string): Promise<RoleMenu[]> {
    return apiClient.get<RoleMenu[]>(`/role-menus/role/${roleId}`)
  }

  /**
   * Get role permissions by menu ID
   */
  async getRolePermissionsByMenu(menuId: string): Promise<RoleMenu[]> {
    return apiClient.get<RoleMenu[]>(`/role-menus/menu/${menuId}`)
  }

  /**
   * Set menu permissions for role
   */
  async setMenuPermissions(request: RoleMenuRequest): Promise<RoleMenu> {
    return apiClient.post<RoleMenu>('/role-menus', request)
  }

  /**
   * Update menu permissions
   */
  async updateMenuPermissions(id: string, request: RoleMenuRequest): Promise<RoleMenu> {
    return apiClient.put<RoleMenu>(`/role-menus/${id}`, request)
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
    return apiClient.post<RoleMenu[]>(
      `/role-menus/role/${targetRoleId}/copy-from/${sourceRoleId}`
    )
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

  /**
   * Validate permission settings before saving
   */
  async validatePermissions(request: RoleMenuRequest): Promise<ValidationResult> {
    return apiClient.post<ValidationResult>('/role-menus/validate', request)
  }

  /**
   * Auto-fix permission dependencies
   */
  async autoFixPermissions(request: RoleMenuRequest): Promise<RoleMenu> {
    return apiClient.post<RoleMenu>('/role-menus/auto-fix', request)
  }

  /**
   * Export role permissions to Excel
   */
  async exportRolePermissions(roleId: string): Promise<Blob> {
    const response = await fetch(`/api/role-menus/role/${roleId}/export`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    })

    if (!response.ok) {
      throw new Error('Failed to export permissions')
    }

    return response.blob()
  }

  /**
   * Export multiple roles permissions to Excel
   */
  async exportMultipleRolePermissions(roleIds: string[]): Promise<Blob> {
    const response = await fetch('/api/role-menus/export-multiple', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(roleIds),
    })

    if (!response.ok) {
      throw new Error('Failed to export permissions')
    }

    return response.blob()
  }

  /**
   * Export all role permissions to Excel
   */
  async exportAllRolePermissions(): Promise<Blob> {
    const response = await fetch('/api/role-menus/export-all', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    })

    if (!response.ok) {
      throw new Error('Failed to export permissions')
    }

    return response.blob()
  }

  /**
   * Helper to download blob as file
   */
  downloadBlob(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  }
}

// Export singleton instance
export const roleMenuApi = new RoleMenuApi()
