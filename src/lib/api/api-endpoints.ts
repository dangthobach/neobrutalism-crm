/**
 * API Endpoint API Service
 * Handles all API endpoint-related API calls
 */

import { apiClient, ApiResponse, PageResponse } from './client'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS'

export interface ApiEndpoint {
  id: string
  method: HttpMethod
  path: string
  tag?: string
  description?: string
  requiresAuth: boolean
  isPublic: boolean
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface ApiEndpointQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
  method?: HttpMethod
  tag?: string
  requiresAuth?: boolean
  isPublic?: boolean
}

export interface CreateApiEndpointRequest {
  method: HttpMethod
  path: string
  tag?: string
  description?: string
  requiresAuth?: boolean
  isPublic?: boolean
}

export interface UpdateApiEndpointRequest {
  method?: HttpMethod
  path?: string
  tag?: string
  description?: string
  requiresAuth?: boolean
  isPublic?: boolean
}

export class ApiEndpointApi {
  /**
   * Get all API endpoints with pagination
   */
  async getApiEndpoints(params?: ApiEndpointQueryParams): Promise<PageResponse<ApiEndpoint>> {
    const response = await apiClient.get<PageResponse<ApiEndpoint>>('/api-endpoints', params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get API endpoint by ID
   */
  async getApiEndpointById(id: string): Promise<ApiEndpoint> {
    const response = await apiClient.get<ApiEndpoint>(`/api-endpoints/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get API endpoints by HTTP method
   */
  async getApiEndpointsByMethod(method: HttpMethod): Promise<ApiEndpoint[]> {
    const response = await apiClient.get<ApiEndpoint[]>('/api-endpoints/by-method', { method })
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get API endpoints by tag
   */
  async getApiEndpointsByTag(tag: string): Promise<ApiEndpoint[]> {
    const response = await apiClient.get<ApiEndpoint[]>('/api-endpoints/by-tag', { tag })
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Get public API endpoints
   */
  async getPublicApiEndpoints(): Promise<ApiEndpoint[]> {
    const response = await apiClient.get<ApiEndpoint[]>('/api-endpoints/public')
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Create new API endpoint
   */
  async createApiEndpoint(request: CreateApiEndpointRequest): Promise<ApiEndpoint> {
    const response = await apiClient.post<ApiEndpoint>('/api-endpoints', request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Update API endpoint
   */
  async updateApiEndpoint(id: string, request: UpdateApiEndpointRequest): Promise<ApiEndpoint> {
    const response = await apiClient.put<ApiEndpoint>(`/api-endpoints/${id}`, request)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  /**
   * Delete API endpoint (soft delete)
   */
  async deleteApiEndpoint(id: string): Promise<void> {
    await apiClient.delete(`/api-endpoints/${id}`)
  }

  /**
   * Check if method + path combination exists
   */
  async checkMethodPath(method: HttpMethod, path: string): Promise<boolean> {
    const response = await apiClient.get<boolean>('/api-endpoints/check', { method, path })
    return response.data ?? false
  }

  /**
   * Get API endpoint by method and path
   */
  async getApiEndpointByMethodAndPath(method: HttpMethod, path: string): Promise<ApiEndpoint> {
    const response = await apiClient.get<ApiEndpoint>('/api-endpoints/find', { method, path })
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }
}

// Export singleton instance
export const apiEndpointApi = new ApiEndpointApi()
