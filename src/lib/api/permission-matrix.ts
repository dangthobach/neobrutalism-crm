/**
 * Permission Matrix API Service
 * Handles permission matrix operations for viewing and managing permissions
 */

import { apiClient, ApiResponse } from './client'

export interface PermissionMatrixDTO {
  tenantId: string
  roles: RoleInfo[]
  resources: ResourceInfo[]
  matrix: Record<string, Record<string, string[]>>
  hierarchy: Record<string, string[]>
}

export interface RoleInfo {
  roleCode: string
  roleName: string
  description?: string
  priority: number
  scope: string
  inherited: boolean
}

export interface ResourceInfo {
  path: string
  name: string
  category: string
  availableActions: string[]
}

export interface PermissionUpdate {
  roleCode: string
  resource: string
  actions: string[]
  reason?: string
}

export interface BulkPermissionUpdate {
  tenantId: string
  updates: PermissionUpdate[]
  reason?: string
}

export class PermissionMatrixApi {
  /**
   * Get permission matrix for a tenant
   */
  async getPermissionMatrix(tenantId: string): Promise<PermissionMatrixDTO> {
    const response = await apiClient.get<PermissionMatrixDTO>(`/permission-matrix/${tenantId}`)
    if (!response) {
      throw new Error('Failed to fetch permission matrix')
    }
    return response
  }

  /**
   * Update permissions in bulk
   */
  async updatePermissionsBulk(update: BulkPermissionUpdate): Promise<number> {
    const response = await apiClient.post<number>('/permission-matrix/bulk-update', update)
    if (response === undefined) {
      throw new Error('Failed to update permissions')
    }
    return response
  }

  /**
   * Update a single permission
   */
  async updatePermission(
    tenantId: string,
    update: PermissionUpdate
  ): Promise<boolean> {
    const response = await apiClient.put<boolean>(
      `/permission-matrix/${tenantId}/permission`,
      update
    )
    return response ?? false
  }

  /**
   * Refresh permission matrix cache
   */
  async refreshMatrix(tenantId: string): Promise<boolean> {
    const response = await apiClient.post<boolean>(
      `/permission-matrix/${tenantId}/refresh`
    )
    return response ?? false
  }
}

// Export singleton instance
export const permissionMatrixApi = new PermissionMatrixApi()

