/**
 * Group API Service
 * Handles all group-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export enum GroupStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
}

export interface Group {
  id: string
  code: string
  name: string
  description?: string
  parentId?: string
  organizationId: string
  level: number
  path: string
  status: GroupStatus
  deleted: boolean
  createdAt: string
  createdBy?: string
  updatedAt?: string
  updatedBy?: string
}

export interface GroupQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export class GroupApi {
  /**
   * Get all groups with pagination
   */
  async getGroups(params?: GroupQueryParams): Promise<PageResponse<Group>> {
    const response = await apiClient.get<PageResponse<Group>>('/groups', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get group by ID
   */
  async getGroupById(id: string): Promise<Group> {
    const response = await apiClient.get<Group>(`/groups/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get group by code
   */
  async getGroupByCode(code: string): Promise<Group> {
    const response = await apiClient.get<Group>(`/groups/code/${code}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get groups by organization
   */
  async getGroupsByOrganization(organizationId: string): Promise<Group[]> {
    const response = await apiClient.get<Group[]>(`/groups/organization/${organizationId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get child groups
   */
  async getChildGroups(parentId: string): Promise<Group[]> {
    const response = await apiClient.get<Group[]>(`/groups/parent/${parentId}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get root groups
   */
  async getRootGroups(): Promise<Group[]> {
    const response = await apiClient.get<Group[]>('/groups/root')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get groups by status
   */
  async getGroupsByStatus(status: GroupStatus): Promise<Group[]> {
    const response = await apiClient.get<Group[]>(`/groups/status/${status}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new group
   */
  async createGroup(group: Partial<Group>): Promise<Group> {
    const response = await apiClient.post<Group>('/groups', group)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update group
   */
  async updateGroup(id: string, group: Partial<Group>): Promise<Group> {
    const response = await apiClient.put<Group>(`/groups/${id}`, group)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Delete group (soft delete)
   */
  async deleteGroup(id: string): Promise<void> {
    await apiClient.delete(`/groups/${id}`)
  }

  /**
   * Activate group
   */
  async activateGroup(id: string): Promise<Group> {
    const response = await apiClient.post<Group>(`/groups/${id}/activate`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Suspend group
   */
  async suspendGroup(id: string): Promise<Group> {
    const response = await apiClient.post<Group>(`/groups/${id}/suspend`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Check code availability
   */
  async checkCode(code: string): Promise<boolean> {
    const response = await apiClient.get<boolean>(`/groups/check-code/${code}`)
    return response.data ?? false
  }
}

// Export singleton instance
export const groupApi = new GroupApi()
