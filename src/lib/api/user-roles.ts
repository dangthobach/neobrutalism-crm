/**
 * UserRole API Service
 * Handles user-role assignments
 */

import { apiClient, ApiResponse } from './client'

export interface UserRole {
  id: string
  userId: string
  roleId: string
  isActive: boolean
  grantedAt: string
  grantedBy?: string
  expiresAt?: string
  createdAt: string
  createdBy?: string
}

export interface UserRoleRequest {
  userId: string
  roleId: string
  grantedBy?: string
  expiresAt?: string
}

export class UserRoleApi {
  /**
   * Get roles by user ID
   */
  async getRolesByUser(userId: string): Promise<UserRole[]> {
    return apiClient.get<UserRole[]>(`/user-roles/user/${userId}`)
  }

  /**
   * Get users by role ID
   */
  async getUsersByRole(roleId: string): Promise<UserRole[]> {
    return apiClient.get<UserRole[]>(`/user-roles/role/${roleId}`)
  }

  /**
   * Assign role to user
   */
  async assignRole(request: UserRoleRequest): Promise<UserRole> {
    return apiClient.post<UserRole>('/user-roles', request)
  }

  /**
   * Update user-role assignment
   */
  async updateUserRole(id: string, request: UserRoleRequest): Promise<UserRole> {
    return apiClient.put<UserRole>(`/user-roles/${id}`, request)
  }

  /**
   * Revoke role from user by ID
   */
  async revokeRole(id: string): Promise<void> {
    await apiClient.delete(`/user-roles/${id}`)
  }

  /**
   * Revoke specific role from user
   */
  async revokeSpecificRole(userId: string, roleId: string): Promise<void> {
    await apiClient.delete(`/user-roles/user/${userId}/role/${roleId}`)
  }

  /**
   * Copy roles from another user
   */
  async copyRolesFromUser(targetUserId: string, sourceUserId: string): Promise<UserRole[]> {
    return apiClient.post<UserRole[]>(
      `/user-roles/user/${targetUserId}/copy-from/${sourceUserId}`
    )
  }

  /**
   * Check if user has role
   */
  async hasRole(userId: string, roleId: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/user-roles/check/${userId}/${roleId}`)
    return response ?? false
  }
}

// Export singleton instance
export const userRoleApi = new UserRoleApi()
