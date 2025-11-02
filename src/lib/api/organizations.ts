/**
 * Organizations API Service
 * Handles all HTTP requests to the backend organization endpoints
 */

import { apiClient, PageResponse } from './client'

export type OrganizationStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "SUSPENDED" | "ARCHIVED"

export interface Organization {
  id: string
  name: string
  code: string
  description?: string
  email?: string
  phone?: string
  website?: string
  address?: string
  status: OrganizationStatus
  deleted: boolean
  createdAt: string
  createdBy: string
  updatedAt: string
  updatedBy: string
}

export interface OrganizationRequest {
  name: string
  code: string
  description?: string
  email?: string
  phone?: string
  website?: string
  address?: string
}

export interface OrganizationReadModel {
  id: string
  name: string
  code: string
  description?: string
  email?: string
  phone?: string
  website?: string
  status: OrganizationStatus
  isActive: boolean
  isDeleted: boolean
  createdAt: string
  createdBy: string
  updatedAt?: string
  updatedBy?: string
  searchText: string
  hasContactInfo: boolean
  daysSinceCreated: number
}

export interface OrganizationStatistics {
  total: number
  active: number
  withContact: number
  deleted: number
}

export interface OrganizationQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

class OrganizationsAPI {
  // ========== WRITE MODEL (CRUD) ==========

  async getAll(): Promise<Organization[]> {
    const response = await apiClient.get<Organization[]>("/organizations")
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async getAllPaged(params?: OrganizationQueryParams): Promise<PageResponse<Organization>> {
    const response = await apiClient.get<PageResponse<Organization>>("/organizations", params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async getById(id: string): Promise<Organization> {
    const response = await apiClient.get<Organization>(`/organizations/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async create(data: OrganizationRequest): Promise<Organization> {
    const response = await apiClient.post<Organization>("/organizations", data)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async update(id: string, data: OrganizationRequest): Promise<Organization> {
    const response = await apiClient.put<Organization>(`/organizations/${id}`, data)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/organizations/${id}`)
  }

  async getByCode(code: string): Promise<Organization> {
    const response = await apiClient.get<Organization>(`/organizations/code/${code}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async getByStatus(status: OrganizationStatus): Promise<Organization[]> {
    const response = await apiClient.get<Organization[]>(`/organizations/status/${status}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  // Status transitions
  async activate(id: string, reason?: string): Promise<Organization> {
    const endpoint = reason ? `/organizations/${id}/activate?reason=${encodeURIComponent(reason)}` : `/organizations/${id}/activate`
    const response = await apiClient.post<Organization>(endpoint)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async suspend(id: string, reason?: string): Promise<Organization> {
    const endpoint = reason ? `/organizations/${id}/suspend?reason=${encodeURIComponent(reason)}` : `/organizations/${id}/suspend`
    const response = await apiClient.post<Organization>(endpoint)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async archive(id: string, reason?: string): Promise<Organization> {
    const endpoint = reason ? `/organizations/${id}/archive?reason=${encodeURIComponent(reason)}` : `/organizations/${id}/archive`
    const response = await apiClient.post<Organization>(endpoint)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  // ========== READ MODEL (CQRS Queries) ==========

  async queryAll(): Promise<OrganizationReadModel[]> {
    // Use the main endpoint with default paging to get all data
    const response = await apiClient.get<PageResponse<OrganizationReadModel>>("/organizations", { page: 0, size: 1000 })
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data.content
  }

  async queryAllPaged(params?: OrganizationQueryParams): Promise<PageResponse<OrganizationReadModel>> {
    const response = await apiClient.get<PageResponse<OrganizationReadModel>>("/organizations", params)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async queryById(id: string): Promise<OrganizationReadModel> {
    const response = await apiClient.get<OrganizationReadModel>(`/organizations/${id}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async queryActive(): Promise<OrganizationReadModel[]> {
    const response = await apiClient.get<OrganizationReadModel[]>(`/organizations/status/ACTIVE`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async queryByStatus(status: OrganizationStatus): Promise<OrganizationReadModel[]> {
    const response = await apiClient.get<OrganizationReadModel[]>(`/organizations/status/${status}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  async queryByCode(code: string): Promise<OrganizationReadModel> {
    const response = await apiClient.get<OrganizationReadModel>(`/organizations/code/${code}`)
    if (!response.data) {
      throw new Error('No data returned from API')
    }
    return response.data
  }

  // Note: These methods are not available in the current backend controller
  // They would need to be implemented in the backend first
  // async search(query: string): Promise<OrganizationReadModel[]> { ... }
  // async queryRecent(days: number): Promise<OrganizationReadModel[]> { ... }
  // async queryWithContactInfo(): Promise<OrganizationReadModel[]> { ... }
  // async queryActiveWithContactInfo(): Promise<OrganizationReadModel[]> { ... }
  // async getStatistics(): Promise<OrganizationStatistics> { ... }
}

export const organizationsAPI = new OrganizationsAPI()
