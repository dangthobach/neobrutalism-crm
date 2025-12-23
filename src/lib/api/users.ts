/**
 * User API Service
 * Handles all user-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export enum UserStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  LOCKED = 'LOCKED',
  INACTIVE = 'INACTIVE',
}

export interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  fullName?: string
  phone?: string
  avatar?: string
  organizationId: string
  status: UserStatus
  lastLoginAt?: string
  lastLoginIp?: string
  deleted: boolean
  createdAt: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
  phone?: string
  avatar?: string
}

export interface UpdateUserRequest {
  username: string
  email: string
  password?: string // Optional - only update if provided
  firstName: string
  lastName: string
  phone?: string
  avatar?: string
}

export interface UserQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface UserSearchRequest {
  keyword?: string
  username?: string
  email?: string
  firstName?: string
  lastName?: string
  organizationId?: string
  status?: UserStatus
  tenantId?: string
  includeDeleted?: boolean
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface UserProfileUpdateRequest {
  firstName: string
  lastName: string
  phone?: string
  avatar?: string
}

export class UserApi {
  /**
   * Get all users with pagination
   */
  async getUsers(params?: UserQueryParams): Promise<PageResponse<User>> {
    return apiClient.get<PageResponse<User>>('/users', params)
  }

  /**
   * Get user by ID
   */
  async getUserById(id: string): Promise<User> {
    return apiClient.get<User>(`/users/${id}`)
  }

  /**
   * Get user by username
   */
  async getUserByUsername(username: string): Promise<User> {
    return apiClient.get<User>(`/users/username/${username}`)
  }

  /**
   * Get user by email
   */
  async getUserByEmail(email: string): Promise<User> {
    return apiClient.get<User>(`/users/email/${email}`)
  }

  /**
   * Get users by organization
   */
  async getUsersByOrganization(organizationId: string): Promise<User[]> {
    return apiClient.get<User[]>(`/users/organization/${organizationId}`)
  }

  /**
   * Get users by status
   */
  async getUsersByStatus(status: UserStatus): Promise<User[]> {
    return apiClient.get<User[]>(`/users/status/${status}`)
  }

  /**
   * Create new user
   */
  async createUser(request: CreateUserRequest): Promise<User> {
    return apiClient.post<User>('/users', request)
  }

  /**
   * Update user
   */
  async updateUser(id: string, request: UpdateUserRequest): Promise<User> {
    return apiClient.put<User>(`/users/${id}`, request)
  }

  /**
   * Delete user (soft delete)
   */
  async deleteUser(id: string): Promise<void> {
    await apiClient.delete(`/users/${id}`)
  }

  /**
   * Activate user
   */
  async activateUser(id: string, reason?: string): Promise<User> {
    return apiClient.post<User>(
      `/users/${id}/activate`,
      reason ? { reason } : undefined
    )
  }

  /**
   * Suspend user
   */
  async suspendUser(id: string, reason?: string): Promise<User> {
    return apiClient.post<User>(
      `/users/${id}/suspend`,
      reason ? { reason } : undefined
    )
  }

  /**
   * Lock user
   */
  async lockUser(id: string, reason?: string): Promise<User> {
    return apiClient.post<User>(
      `/users/${id}/lock`,
      reason ? { reason } : undefined
    )
  }

  /**
   * Unlock user
   */
  async unlockUser(id: string, reason?: string): Promise<User> {
    return apiClient.post<User>(
      `/users/${id}/unlock`,
      reason ? { reason } : undefined
    )
  }

  /**
   * Check username availability
   */
  async checkUsername(username: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/users/check-username/${username}`)
    return response ?? false
  }

  /**
   * Check email availability
   */
  async checkEmail(email: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/users/check-email/${email}`)
    return response ?? false
  }

  /**
   * Search users with advanced filters
   */
  async searchUsers(request: UserSearchRequest): Promise<PageResponse<User>> {
    return apiClient.post<PageResponse<User>>('/users/search', request)
  }

  /**
   * Restore deleted user
   */
  async restoreUser(id: string): Promise<User> {
    return apiClient.post<User>(`/users/${id}/restore`)
  }

  /**
   * Get current user profile
   */
  async getCurrentUserProfile(): Promise<User> {
    // apiClient.get already unwraps ApiResponse, so response is User directly
    const response = await apiClient.get<User>('/users/me')
    return response
  }

  /**
   * Update current user profile
   */
  async updateCurrentUserProfile(request: UserProfileUpdateRequest): Promise<User> {
    return apiClient.put<User>('/users/me/profile', request)
  }

  /**
   * Get current user's menus
   */
  async getCurrentUserMenus(): Promise<any[]> {
    return apiClient.get<any[]>('/users/me/menus')
  }

  /**
   * Get user's menus by ID
   */
  async getUserMenus(id: string): Promise<any[]> {
    return apiClient.get<any[]>(`/users/${id}/menus`)
  }
}

// Export singleton instance
export const userApi = new UserApi()
