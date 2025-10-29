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
  organizationId: string
}

export interface UpdateUserRequest {
  username: string
  email: string
  password?: string // Optional - only update if provided
  firstName: string
  lastName: string
  phone?: string
  avatar?: string
  organizationId: string
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
    const response = await apiClient.get<PageResponse<User>>('/users', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get user by ID
   */
  async getUserById(id: string): Promise<User> {
    const response = await apiClient.get<User>(`/users/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get user by username
   */
  async getUserByUsername(username: string): Promise<User> {
    const response = await apiClient.get<User>(`/users/username/${username}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get user by email
   */
  async getUserByEmail(email: string): Promise<User> {
    const response = await apiClient.get<User>(`/users/email/${email}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get users by organization
   */
  async getUsersByOrganization(organizationId: string): Promise<User[]> {
    const response = await apiClient.get<User[]>(`/users/organization/${organizationId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get users by status
   */
  async getUsersByStatus(status: UserStatus): Promise<User[]> {
    const response = await apiClient.get<User[]>(`/users/status/${status}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new user
   */
  async createUser(request: CreateUserRequest): Promise<User> {
    const response = await apiClient.post<User>('/users', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update user
   */
  async updateUser(id: string, request: UpdateUserRequest): Promise<User> {
    const response = await apiClient.put<User>(`/users/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
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
    const response = await apiClient.post<User>(
      `/users/${id}/activate`,
      reason ? { reason } : undefined
    )
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Suspend user
   */
  async suspendUser(id: string, reason?: string): Promise<User> {
    const response = await apiClient.post<User>(
      `/users/${id}/suspend`,
      reason ? { reason } : undefined
    )
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Lock user
   */
  async lockUser(id: string, reason?: string): Promise<User> {
    const response = await apiClient.post<User>(
      `/users/${id}/lock`,
      reason ? { reason } : undefined
    )
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Unlock user
   */
  async unlockUser(id: string, reason?: string): Promise<User> {
    const response = await apiClient.post<User>(
      `/users/${id}/unlock`,
      reason ? { reason } : undefined
    )
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Check username availability
   */
  async checkUsername(username: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/users/check-username/${username}`)
    return response.data ?? false
  }

  /**
   * Check email availability
   */
  async checkEmail(email: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/users/check-email/${email}`)
    return response.data ?? false
  }

  /**
   * Search users with advanced filters
   */
  async searchUsers(request: UserSearchRequest): Promise<PageResponse<User>> {
    const response = await apiClient.post<PageResponse<User>>('/users/search', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Restore deleted user
   */
  async restoreUser(id: string): Promise<User> {
    const response = await apiClient.post<User>(`/users/${id}/restore`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get current user profile
   */
  async getCurrentUserProfile(): Promise<User> {
    const response = await apiClient.get<User>('/users/me')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update current user profile
   */
  async updateCurrentUserProfile(request: UserProfileUpdateRequest): Promise<User> {
    const response = await apiClient.put<User>('/users/me/profile', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }
}

// Export singleton instance
export const userApi = new UserApi()
