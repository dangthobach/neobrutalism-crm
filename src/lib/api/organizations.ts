/**
 * Organizations API Service
 * Handles all HTTP requests to the backend organization endpoints
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"
const TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID || "default"

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

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  errorCode?: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  pageIndex: number
  pageSize: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
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

class OrganizationsAPI {
  private async fetchAPI<T>(endpoint: string, options?: RequestInit): Promise<ApiResponse<T>> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      headers: {
        "Content-Type": "application/json",
        "X-Tenant-ID": TENANT_ID,
        ...options?.headers,
      },
      ...options,
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: "Network error" }))
      throw new Error(error.message || `HTTP ${response.status}`)
    }

    return response.json()
  }

  // ========== WRITE MODEL (CRUD) ==========

  async getAll(): Promise<Organization[]> {
    const response = await this.fetchAPI<Organization[]>("/organizations")
    return response.data
  }

  async getAllPaged(page: number = 0, size: number = 20, sortBy: string = "id", sortDirection: "ASC" | "DESC" = "ASC"): Promise<PageResponse<Organization>> {
    const response = await this.fetchAPI<PageResponse<Organization>>(
      `/organizations?page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`
    )
    return response.data
  }

  async getById(id: string): Promise<Organization> {
    const response = await this.fetchAPI<Organization>(`/organizations/${id}`)
    return response.data
  }

  async create(data: OrganizationRequest): Promise<Organization> {
    const response = await this.fetchAPI<Organization>("/organizations", {
      method: "POST",
      body: JSON.stringify(data),
    })
    return response.data
  }

  async update(id: string, data: OrganizationRequest): Promise<Organization> {
    const response = await this.fetchAPI<Organization>(`/organizations/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    })
    return response.data
  }

  async delete(id: string): Promise<void> {
    await this.fetchAPI<void>(`/organizations/${id}`, {
      method: "DELETE",
    })
  }

  async getByCode(code: string): Promise<Organization> {
    const response = await this.fetchAPI<Organization>(`/organizations/code/${code}`)
    return response.data
  }

  async getByStatus(status: OrganizationStatus): Promise<Organization[]> {
    const response = await this.fetchAPI<Organization[]>(`/organizations/status/${status}`)
    return response.data
  }

  // Status transitions
  async activate(id: string, reason?: string): Promise<Organization> {
    const url = reason ? `/organizations/${id}/activate?reason=${encodeURIComponent(reason)}` : `/organizations/${id}/activate`
    const response = await this.fetchAPI<Organization>(url, {
      method: "POST",
    })
    return response.data
  }

  async suspend(id: string, reason?: string): Promise<Organization> {
    const url = reason ? `/organizations/${id}/suspend?reason=${encodeURIComponent(reason)}` : `/organizations/${id}/suspend`
    const response = await this.fetchAPI<Organization>(url, {
      method: "POST",
    })
    return response.data
  }

  async archive(id: string, reason?: string): Promise<Organization> {
    const url = reason ? `/organizations/${id}/archive?reason=${encodeURIComponent(reason)}` : `/organizations/${id}/archive`
    const response = await this.fetchAPI<Organization>(url, {
      method: "POST",
    })
    return response.data
  }

  // ========== READ MODEL (CQRS Queries) ==========

  async queryAll(): Promise<OrganizationReadModel[]> {
    // Use the main endpoint with default paging to get all data
    const response = await this.fetchAPI<PageResponse<OrganizationReadModel>>("/organizations?page=0&size=1000")
    return response.data.content
  }

  async queryAllPaged(page: number = 0, size: number = 20, sortBy: string = "id", sortDirection: "ASC" | "DESC" = "ASC"): Promise<PageResponse<OrganizationReadModel>> {
    const response = await this.fetchAPI<PageResponse<OrganizationReadModel>>(
      `/organizations?page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`
    )
    return response.data
  }

  async queryById(id: string): Promise<OrganizationReadModel> {
    const response = await this.fetchAPI<OrganizationReadModel>(`/organizations/${id}`)
    return response.data
  }

  async queryActive(): Promise<OrganizationReadModel[]> {
    // Use status filter to get active organizations
    const response = await this.fetchAPI<OrganizationReadModel[]>(`/organizations/status/ACTIVE`)
    return response.data
  }

  async queryByStatus(status: OrganizationStatus): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>(`/organizations/status/${status}`)
    return response.data
  }

  async queryByCode(code: string): Promise<OrganizationReadModel> {
    const response = await this.fetchAPI<OrganizationReadModel>(`/organizations/code/${code}`)
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
