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
    return apiClient.get<PageResponse<ApiEndpoint>>('/api-endpoints', params)
  }

  /**
   * Get API endpoint by ID
   */
  async getApiEndpointById(id: string): Promise<ApiEndpoint> {
    return apiClient.get<ApiEndpoint>(`/api-endpoints/${id}`)
  }

  /**
   * Get API endpoints by HTTP method
   */
  async getApiEndpointsByMethod(method: HttpMethod): Promise<ApiEndpoint[]> {
    return apiClient.get<ApiEndpoint[]>('/api-endpoints/by-method', { method })
  }

  /**
   * Get API endpoints by tag
   */
  async getApiEndpointsByTag(tag: string): Promise<ApiEndpoint[]> {
    return apiClient.get<ApiEndpoint[]>('/api-endpoints/by-tag', { tag })
  }

  /**
   * Get public API endpoints
   */
  async getPublicApiEndpoints(): Promise<ApiEndpoint[]> {
    return apiClient.get<ApiEndpoint[]>('/api-endpoints/public')
  }

  /**
   * Create new API endpoint
   */
  async createApiEndpoint(request: CreateApiEndpointRequest): Promise<ApiEndpoint> {
    return apiClient.post<ApiEndpoint>('/api-endpoints', request)
  }

  /**
   * Update API endpoint
   */
  async updateApiEndpoint(id: string, request: UpdateApiEndpointRequest): Promise<ApiEndpoint> {
    return apiClient.put<ApiEndpoint>(`/api-endpoints/${id}`, request)
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
    return response ?? false
  }

  /**
   * Get API endpoint by method and path
   */
  async getApiEndpointByMethodAndPath(method: HttpMethod, path: string): Promise<ApiEndpoint> {
    return apiClient.get<ApiEndpoint>('/api-endpoints/find', { method, path })
  }
}

// Export singleton instance
export const apiEndpointApi = new ApiEndpointApi()
