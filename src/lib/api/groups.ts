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
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Get group by ID
   */
  async getGroupById(id: string): Promise<Group> {
    const response = await apiClient.get<Group>(`/groups/${id}`)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Get group by code
   */
  async getGroupByCode(code: string): Promise<Group> {
    const response = await apiClient.get<Group>(`/groups/code/${code}`)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Get groups by organization
   */
  async getGroupsByOrganization(organizationId: string): Promise<Group[]> {
    const response = await apiClient.get<Group[]>(`/groups/organization/${organizationId}`)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Get child groups
   */
  async getChildGroups(parentId: string): Promise<Group[]> {
    const response = await apiClient.get<Group[]>(`/groups/parent/${parentId}`)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Get root groups
   */
  async getRootGroups(): Promise<Group[]> {
    const response = await apiClient.get<Group[]>('/groups/root')
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Get groups by status
   */
  async getGroupsByStatus(status: GroupStatus): Promise<Group[]> {
    const response = await apiClient.get<Group[]>(`/groups/status/${status}`)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Create new group
   */
  async createGroup(group: Partial<Group>): Promise<Group> {
    const response = await apiClient.post<Group>('/groups', group)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Update group
   */
  async updateGroup(id: string, group: Partial<Group>): Promise<Group> {
    const response = await apiClient.put<Group>(`/groups/${id}`, group)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
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
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
  }

  /**
   * Suspend group
   */
  async suspendGroup(id: string): Promise<Group> {
    const response = await apiClient.post<Group>(`/groups/${id}/suspend`)
    if (!response) {
      throw new Error('No data returned from API')
    }
    return response
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
