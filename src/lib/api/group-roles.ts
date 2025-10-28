/**
 * GroupRole API Service
 * Handles group-role assignments
 */

import { apiClient, ApiResponse } from './client'

export interface GroupRole {
  id: string
  groupId: string
  roleId: string
  grantedAt: string
  createdAt: string
  createdBy?: string
}

export interface GroupRoleRequest {
  groupId: string
  roleId: string
}

export class GroupRoleApi {
  /**
   * Get roles by group ID
   */
  async getRolesByGroup(groupId: string): Promise<GroupRole[]> {
    const response = await apiClient.get<GroupRole[]>(`/group-roles/group/${groupId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get groups by role ID
   */
  async getGroupsByRole(roleId: string): Promise<GroupRole[]> {
    const response = await apiClient.get<GroupRole[]>(`/group-roles/role/${roleId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Assign role to group
   */
  async assignRoleToGroup(request: GroupRoleRequest): Promise<GroupRole> {
    const response = await apiClient.post<GroupRole>('/group-roles', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Revoke role from group by ID
   */
  async revokeRoleFromGroup(id: string): Promise<void> {
    await apiClient.delete(`/group-roles/${id}`)
  }

  /**
   * Revoke specific role from group
   */
  async revokeSpecificRoleFromGroup(groupId: string, roleId: string): Promise<void> {
    await apiClient.delete(`/group-roles/group/${groupId}/role/${roleId}`)
  }
}

// Export singleton instance
export const groupRoleApi = new GroupRoleApi()
