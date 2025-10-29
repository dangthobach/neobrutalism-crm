/**
 * Role API Service
 * Handles all role-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export enum RoleStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
}

export interface Role {
  id: string
  code: string
  name: string
  description?: string
  organizationId: string
  isSystem: boolean
  priority: number
  status: RoleStatus
  deleted: boolean
  createdAt: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

export interface CreateRoleRequest {
  code: string
  name: string
  description?: string
  organizationId: string
  isSystem?: boolean
  priority?: number
}

export interface UpdateRoleRequest {
  code: string
  name: string
  description?: string
  organizationId: string
  isSystem?: boolean
  priority?: number
}

export interface RoleQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export class RoleApi {
  /**
   * Get all roles with pagination
   */
  async getRoles(params?: RoleQueryParams): Promise<PageResponse<Role>> {
    const response = await apiClient.get<PageResponse<Role>>('/roles', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get role by ID
   */
  async getRoleById(id: string): Promise<Role> {
    const response = await apiClient.get<Role>(`/roles/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get role by code
   */
  async getRoleByCode(code: string): Promise<Role> {
    const response = await apiClient.get<Role>(`/roles/code/${code}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get roles by organization
   */
  async getRolesByOrganization(organizationId: string): Promise<Role[]> {
    const response = await apiClient.get<Role[]>(`/roles/organization/${organizationId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get roles by status
   */
  async getRolesByStatus(status: RoleStatus): Promise<Role[]> {
    const response = await apiClient.get<Role[]>(`/roles/status/${status}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get system roles
   */
  async getSystemRoles(): Promise<Role[]> {
    const response = await apiClient.get<Role[]>('/roles/system')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new role
   */
  async createRole(request: CreateRoleRequest): Promise<Role> {
    const response = await apiClient.post<Role>('/roles', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update role
   */
  async updateRole(id: string, request: UpdateRoleRequest): Promise<Role> {
    const response = await apiClient.put<Role>(`/roles/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Delete role (soft delete)
   */
  async deleteRole(id: string): Promise<void> {
    await apiClient.delete(`/roles/${id}`)
  }

  /**
   * Activate role
   */
  async activateRole(id: string): Promise<Role> {
    const response = await apiClient.post<Role>(`/roles/${id}/activate`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Suspend role
   */
  async suspendRole(id: string): Promise<Role> {
    const response = await apiClient.post<Role>(`/roles/${id}/suspend`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Check code availability
   */
  async checkCode(code: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/roles/check-code/${code}`)
    return response.data ?? false
  }
}

// Export singleton instance
export const roleApi = new RoleApi()
