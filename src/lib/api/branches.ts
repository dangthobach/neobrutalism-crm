/**
 * Branch API Client
 * Handles all branch-related API calls
 */

import { apiClient, PageResponse } from './client'
import type {
  Branch,
  CreateBranchRequest,
  UpdateBranchRequest,
  BranchSearchParams,
} from '@/types/branch'

export const branchApi = {
  /**
   * Get paginated list of branches
   */
  getAll: async (params?: BranchSearchParams): Promise<PageResponse<Branch>> => {
    const response = await apiClient.get<PageResponse<Branch>>('/branches', params)
    return response.data!
  },

  /**
   * Get branch by ID
   */
  getById: async (id: string): Promise<Branch> => {
    const response = await apiClient.get<Branch>(`/branches/${id}`)
    return response.data!
  },

  /**
   * Get branch by code
   */
  getByCode: async (code: string): Promise<Branch> => {
    const response = await apiClient.get<Branch>(`/branches/code/${code}`)
    return response.data!
  },

  /**
   * Create new branch
   */
  create: async (data: CreateBranchRequest): Promise<Branch> => {
    const response = await apiClient.post<Branch>('/branches', data)
    return response.data!
  },

  /**
   * Update branch
   */
  update: async (id: string, data: UpdateBranchRequest): Promise<Branch> => {
    const response = await apiClient.put<Branch>(`/branches/${id}`, data)
    return response.data!
  },

  /**
   * Delete branch
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/branches/${id}`)
  },

  /**
   * Get branches by organization
   */
  getByOrganization: async (organizationId: string, params?: BranchSearchParams): Promise<PageResponse<Branch>> => {
    const response = await apiClient.get<PageResponse<Branch>>(`/branches/organization/${organizationId}`, params)
    return response.data!
  },

  /**
   * Get active branches
   */
  getActive: async (params?: BranchSearchParams): Promise<PageResponse<Branch>> => {
    const response = await apiClient.get<PageResponse<Branch>>('/branches/active', params)
    return response.data!
  },

  /**
   * Search branches by keyword
   */
  search: async (keyword: string): Promise<Branch[]> => {
    const response = await apiClient.get<Branch[]>('/branches/search', { keyword })
    return response.data!
  },
}
