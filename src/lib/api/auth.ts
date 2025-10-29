/**
 * Authentication API Service
 * Handles login, logout, and token management
 */

import { apiClient, ApiResponse } from './client'

export interface LoginRequest {
  username: string
  password: string
  tenantId?: string
  rememberMe?: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: {
    id: string
    username: string
    email: string
    firstName: string
    lastName: string
    fullName?: string
    avatar?: string
    organizationId: string
    roles: string[]
    permissions: string[]
  }
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export class AuthApi {
  /**
   * Login user
   */
  async login(request: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/auth/login', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Refresh access token
   */
  async refreshToken(request: RefreshTokenRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/auth/refresh', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Change password
   */
  async changePassword(request: ChangePasswordRequest): Promise<void> {
    await apiClient.post<void>('/auth/change-password', request)
  }

  /**
   * Logout user
   */
  async logout(): Promise<void> {
    await apiClient.post<void>('/auth/logout')
  }

  /**
   * Get current user info
   */
  async getCurrentUser(): Promise<string> {
    const response = await apiClient.get<string>('/auth/me')
    return response.data || ''
  }
}

// Export singleton instance
export const authApi = new AuthApi()
