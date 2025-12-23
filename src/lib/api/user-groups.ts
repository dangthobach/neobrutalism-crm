/**
 * UserGroup API Service
 * Handles user-group assignments
 */

import { apiClient, ApiResponse } from './client'

export interface UserGroup {
  id: string
  userId: string
  groupId: string
  isPrimary: boolean
  joinedAt: string
  createdAt: string
  createdBy?: string
}

export interface UserGroupRequest {
  userId: string
  groupId: string
  isPrimary?: boolean
}

export class UserGroupApi {
  /**
   * Get groups by user ID
   */
  async getGroupsByUser(userId: string): Promise<UserGroup[]> {
    return apiClient.get<UserGroup[]>(`/user-groups/user/${userId}`)
  }

  /**
   * Get users by group ID
   */
  async getUsersByGroup(groupId: string): Promise<UserGroup[]> {
    return apiClient.get<UserGroup[]>(`/user-groups/group/${groupId}`)
  }

  /**
   * Get user's primary group
   */
  async getPrimaryGroup(userId: string): Promise<UserGroup> {
    return apiClient.get<UserGroup>(`/user-groups/user/${userId}/primary`)
  }

  /**
   * Assign user to group
   */
  async assignUserToGroup(request: UserGroupRequest): Promise<UserGroup> {
    return apiClient.post<UserGroup>('/user-groups', request)
  }

  /**
   * Update user-group assignment
   */
  async updateUserGroup(id: string, request: UserGroupRequest): Promise<UserGroup> {
    return apiClient.put<UserGroup>(`/user-groups/${id}`, request)
  }

  /**
   * Remove user from group by ID
   */
  async removeUserFromGroup(id: string): Promise<void> {
    await apiClient.delete(`/user-groups/${id}`)
  }

  /**
   * Remove specific user from group
   */
  async removeSpecificUserFromGroup(userId: string, groupId: string): Promise<void> {
    await apiClient.delete(`/user-groups/user/${userId}/group/${groupId}`)
  }

  /**
   * Set as primary group
   */
  async setPrimaryGroup(id: string): Promise<UserGroup> {
    return apiClient.post<UserGroup>(`/user-groups/${id}/set-primary`)
  }
}

// Export singleton instance
export const userGroupApi = new UserGroupApi()
