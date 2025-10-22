/**
 * Organizations API Service
 * Handles all HTTP requests to the backend organization endpoints
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

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
  async activate(id: string, reason: string): Promise<Organization> {
    const response = await this.fetchAPI<Organization>(`/organizations/${id}/activate?reason=${encodeURIComponent(reason)}`, {
      method: "PUT",
    })
    return response.data
  }

  async suspend(id: string, reason: string): Promise<Organization> {
    const response = await this.fetchAPI<Organization>(`/organizations/${id}/suspend?reason=${encodeURIComponent(reason)}`, {
      method: "PUT",
    })
    return response.data
  }

  async archive(id: string, reason: string): Promise<Organization> {
    const response = await this.fetchAPI<Organization>(`/organizations/${id}/archive?reason=${encodeURIComponent(reason)}`, {
      method: "PUT",
    })
    return response.data
  }

  // ========== READ MODEL (CQRS Queries) ==========

  async queryAll(): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>("/organizations/query/all")
    return response.data
  }

  async queryById(id: string): Promise<OrganizationReadModel> {
    const response = await this.fetchAPI<OrganizationReadModel>(`/organizations/query/${id}`)
    return response.data
  }

  async queryActive(): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>("/organizations/query/active")
    return response.data
  }

  async queryByStatus(status: OrganizationStatus): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>(`/organizations/query/status/${status}`)
    return response.data
  }

  async queryByCode(code: string): Promise<OrganizationReadModel> {
    const response = await this.fetchAPI<OrganizationReadModel>(`/organizations/query/code/${code}`)
    return response.data
  }

  async search(query: string): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>(`/organizations/query/search?query=${encodeURIComponent(query)}`)
    return response.data
  }

  async queryRecent(days: number): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>(`/organizations/query/recent/${days}`)
    return response.data
  }

  async queryWithContactInfo(): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>("/organizations/query/with-contact")
    return response.data
  }

  async queryActiveWithContactInfo(): Promise<OrganizationReadModel[]> {
    const response = await this.fetchAPI<OrganizationReadModel[]>("/organizations/query/active-with-contact")
    return response.data
  }

  async getStatistics(): Promise<OrganizationStatistics> {
    const response = await this.fetchAPI<OrganizationStatistics>("/organizations/query/statistics")
    return response.data
  }
}

export const organizationsAPI = new OrganizationsAPI()
