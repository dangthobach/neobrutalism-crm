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
    const response = await apiClient.get<UserGroup[]>(`/user-groups/user/${userId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get users by group ID
   */
  async getUsersByGroup(groupId: string): Promise<UserGroup[]> {
    const response = await apiClient.get<UserGroup[]>(`/user-groups/group/${groupId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get user's primary group
   */
  async getPrimaryGroup(userId: string): Promise<UserGroup> {
    const response = await apiClient.get<UserGroup>(`/user-groups/user/${userId}/primary`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Assign user to group
   */
  async assignUserToGroup(request: UserGroupRequest): Promise<UserGroup> {
    const response = await apiClient.post<UserGroup>('/user-groups', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update user-group assignment
   */
  async updateUserGroup(id: string, request: UserGroupRequest): Promise<UserGroup> {
    const response = await apiClient.put<UserGroup>(`/user-groups/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
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
    const response = await apiClient.post<UserGroup>(`/user-groups/${id}/set-primary`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }
}

// Export singleton instance
export const userGroupApi = new UserGroupApi()
