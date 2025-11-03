/**
 * Organizations API Service
 * Handles all HTTP requests to the backend organization endpoints
 * 
 * ⚠️ CRITICAL: This file is synchronized with backend endpoints
 * Backend: OrganizationController.java
 * 
 * Available Endpoints:
 * - GET    /api/organizations              - Get all (paginated)
 * - GET    /api/organizations/{id}         - Get by ID
 * - GET    /api/organizations/code/{code}  - Get by code
 * - POST   /api/organizations              - Create
 * - PUT    /api/organizations/{id}         - Update
 * - DELETE /api/organizations/{id}         - Delete (soft)
 * - POST   /api/organizations/{id}/activate - Activate
 * - POST   /api/organizations/{id}/suspend  - Suspend
 * - POST   /api/organizations/{id}/archive  - Archive
 * - GET    /api/organizations/status/{status} - Get by status
 */

import { apiClient, PageResponse } from './client'

/**
 * Organization Status Enum
 * MUST match backend: OrganizationStatus.java
 */
export type OrganizationStatus = 
  | "DRAFT"      // Initial state
  | "ACTIVE"     // Fully operational
  | "INACTIVE"   // Temporarily inactive
  | "SUSPENDED"  // Suspended by admin
  | "ARCHIVED"   // Permanently archived

/**
 * Organization Entity (Write Model)
 * Matches backend: OrganizationResponse.java
 */
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

/**
 * Organization Request DTO
 * Matches backend: OrganizationRequest.java
 * 
 * Validation Rules:
 * - name: 2-200 characters, required
 * - code: 2-50 characters, uppercase alphanumeric with dashes/underscores, required
 * - description: max 1000 characters, optional
 * - email: valid email format, optional
 * - phone: valid phone format, optional
 * - website: valid URL format, optional
 * - address: max 500 characters, optional
 */
export interface OrganizationRequest {
  name: string          // Length: 2-200
  code: string          // Pattern: ^[A-Z0-9_-]+$, Length: 2-50
  description?: string  // Max: 1000
  email?: string        // Format: email
  phone?: string        // Format: phone
  website?: string      // Format: URL
  address?: string      // Max: 500
}

/**
 * Query Parameters for pagination
 */
export interface OrganizationQueryParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

/**
 * Utility Types for better DX
 */
export type OrganizationId = Organization['id']
export type OrganizationCode = Organization['code']
export type OrganizationCreateInput = Omit<Organization, 'id' | 'status' | 'deleted' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>
export type OrganizationUpdateInput = Partial<OrganizationRequest>

/**
 * Type guard to check if value is a valid OrganizationStatus
 */
export function isValidOrganizationStatus(value: any): value is OrganizationStatus {
  const validStatuses: OrganizationStatus[] = ['DRAFT', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'ARCHIVED']
  return validStatuses.includes(value)
}

/**
 * Validation helper for organization code format
 */
export function isValidOrganizationCode(code: string): boolean {
  const codeRegex = /^[A-Z0-9_-]+$/
  return code.length >= 2 && code.length <= 50 && codeRegex.test(code)
}

/**
 * Validation helper for organization name
 */
export function isValidOrganizationName(name: string): boolean {
  return name.length >= 2 && name.length <= 200
}

/**
 * Validation helper for UUID format
 */
export function isValidUUID(id: string): boolean {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
  return uuidRegex.test(id)
}

/**
 * Organizations API Class
 * All methods are aligned with backend OrganizationController.java
 */
class OrganizationsAPI {
  private readonly BASE_PATH = "/organizations"

  // ========== CRUD Operations ==========

  /**
   * Get all organizations with pagination
   * Backend: GET /api/organizations
   * 
   * @param params - Query parameters for pagination and sorting
   * @returns Paginated list of organizations
   * @throws {Error} If request fails
   */
  async getAll(params?: OrganizationQueryParams): Promise<PageResponse<Organization>> {
    try {
      return await apiClient.get<PageResponse<Organization>>(this.BASE_PATH, params)
    } catch (error: any) {
      console.error('❌ Failed to fetch organizations:', { params, error: error.message })
      throw new Error(`Failed to fetch organizations: ${error.message}`)
    }
  }

  /**
   * Get organization by ID
   * Backend: GET /api/organizations/{id}
   * 
   * @param id - Organization UUID
   * @returns Organization details
   * @throws {Error} If ID is invalid or not found
   */
  async getById(id: string): Promise<Organization> {
    // Input validation
    if (!id || id.trim() === '') {
      throw new Error('Organization ID is required')
    }

    // Validate UUID format (loose validation)
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    if (!uuidRegex.test(id)) {
      throw new Error('Invalid organization ID format. Expected UUID.')
    }

    try {
      return await apiClient.get<Organization>(`${this.BASE_PATH}/${id}`)
    } catch (error: any) {
      console.error('❌ Failed to fetch organization:', { id, error: error.message })
      throw new Error(`Failed to fetch organization ${id}: ${error.message}`)
    }
  }

  /**
   * Get organization by code
   * Backend: GET /api/organizations/code/{code}
   * 
   * @param code - Organization code (uppercase alphanumeric with dashes/underscores)
   * @returns Organization details
   * @throws {Error} If code is invalid or not found
   */
  async getByCode(code: string): Promise<Organization> {
    // Input validation
    if (!code || code.trim() === '') {
      throw new Error('Organization code is required')
    }

    // Validate code format (must match backend pattern)
    const codeRegex = /^[A-Z0-9_-]+$/
    if (!codeRegex.test(code)) {
      throw new Error('Invalid organization code format. Only uppercase letters, numbers, dashes, and underscores are allowed.')
    }

    try {
      return await apiClient.get<Organization>(`${this.BASE_PATH}/code/${code}`)
    } catch (error: any) {
      console.error('❌ Failed to fetch organization by code:', { code, error: error.message })
      throw new Error(`Failed to fetch organization with code ${code}: ${error.message}`)
    }
  }

  /**
   * Create new organization
   * Backend: POST /api/organizations
   * 
   * @param data - Organization data
   * @returns Created organization
   * @throws {Error} If validation fails or request fails
   */
  async create(data: OrganizationRequest): Promise<Organization> {
    // Input validation
    if (!data) {
      throw new Error('Organization data is required')
    }

    if (!data.name || data.name.trim() === '') {
      throw new Error('Organization name is required')
    }

    if (!data.code || data.code.trim() === '') {
      throw new Error('Organization code is required')
    }

    // Validate name length
    if (data.name.length < 2 || data.name.length > 200) {
      throw new Error('Organization name must be between 2 and 200 characters')
    }

    // Validate code format
    const codeRegex = /^[A-Z0-9_-]+$/
    if (!codeRegex.test(data.code)) {
      throw new Error('Organization code must contain only uppercase letters, numbers, dashes, and underscores')
    }

    if (data.code.length < 2 || data.code.length > 50) {
      throw new Error('Organization code must be between 2 and 50 characters')
    }

    try {
      const result = await apiClient.post<Organization>(this.BASE_PATH, data)
      
      if (process.env.NODE_ENV === 'development') {
        console.log('✅ Organization created:', { id: result.id, name: result.name })
      }
      
      return result
    } catch (error: any) {
      console.error('❌ Failed to create organization:', { data, error: error.message })
      throw new Error(`Failed to create organization: ${error.message}`)
    }
  }

  /**
   * Update organization
   * Backend: PUT /api/organizations/{id}
   * 
   * @param id - Organization ID
   * @param data - Updated organization data
   * @returns Updated organization
   * @throws {Error} If validation fails or request fails
   */
  async update(id: string, data: OrganizationRequest): Promise<Organization> {
    // Input validation
    if (!id || id.trim() === '') {
      throw new Error('Organization ID is required')
    }

    if (!data) {
      throw new Error('Organization data is required')
    }

    if (!data.name || data.name.trim() === '') {
      throw new Error('Organization name is required')
    }

    if (!data.code || data.code.trim() === '') {
      throw new Error('Organization code is required')
    }

    // Validate UUID format
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    if (!uuidRegex.test(id)) {
      throw new Error('Invalid organization ID format. Expected UUID.')
    }

    // Validate name length
    if (data.name.length < 2 || data.name.length > 200) {
      throw new Error('Organization name must be between 2 and 200 characters')
    }

    // Validate code format
    const codeRegex = /^[A-Z0-9_-]+$/
    if (!codeRegex.test(data.code)) {
      throw new Error('Organization code must contain only uppercase letters, numbers, dashes, and underscores')
    }

    try {
      const result = await apiClient.put<Organization>(`${this.BASE_PATH}/${id}`, data)
      
      if (process.env.NODE_ENV === 'development') {
        console.log('✅ Organization updated:', { id, name: result.name })
      }
      
      return result
    } catch (error: any) {
      console.error('❌ Failed to update organization:', { id, data, error: error.message })
      throw new Error(`Failed to update organization ${id}: ${error.message}`)
    }
  }

  /**
   * Delete organization (soft delete)
   * Backend: DELETE /api/organizations/{id}
   * 
   * @param id - Organization ID
   * @throws {Error} If ID is invalid or request fails
   */
  async delete(id: string): Promise<void> {
    // Input validation
    if (!id || id.trim() === '') {
      throw new Error('Organization ID is required')
    }

    // Validate UUID format
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    if (!uuidRegex.test(id)) {
      throw new Error('Invalid organization ID format. Expected UUID.')
    }

    try {
      await apiClient.delete<void>(`${this.BASE_PATH}/${id}`)
      
      if (process.env.NODE_ENV === 'development') {
        console.log('✅ Organization deleted:', { id })
      }
    } catch (error: any) {
      console.error('❌ Failed to delete organization:', { id, error: error.message })
      throw new Error(`Failed to delete organization ${id}: ${error.message}`)
    }
  }

  // ========== Status Operations ==========

  /**
   * Activate organization
   * Backend: POST /api/organizations/{id}/activate?reason={reason}
   * 
   * @param id - Organization ID
   * @param reason - Optional reason for activation
   * @throws {Error} If ID is invalid or request fails
   */
  async activate(id: string, reason?: string): Promise<Organization> {
    // Input validation
    if (!id || id.trim() === '') {
      throw new Error('Organization ID is required')
    }

    try {
      // Build URL with query parameter (not body)
      const endpoint = reason 
        ? `${this.BASE_PATH}/${id}/activate?reason=${encodeURIComponent(reason)}`
        : `${this.BASE_PATH}/${id}/activate`
      
      const result = await apiClient.post<Organization>(endpoint)
      
      if (process.env.NODE_ENV === 'development') {
        console.log('✅ Organization activated:', { id, reason })
      }
      
      return result
    } catch (error: any) {
      console.error('❌ Failed to activate organization:', {
        id,
        reason,
        error: error.message,
      })
      throw new Error(`Failed to activate organization ${id}: ${error.message}`)
    }
  }

  /**
   * Suspend organization
   * Backend: POST /api/organizations/{id}/suspend?reason={reason}
   * 
   * @param id - Organization ID
   * @param reason - Optional reason for suspension
   * @throws {Error} If ID is invalid or request fails
   */
  async suspend(id: string, reason?: string): Promise<Organization> {
    // Input validation
    if (!id || id.trim() === '') {
      throw new Error('Organization ID is required')
    }

    try {
      // Build URL with query parameter (not body)
      const endpoint = reason 
        ? `${this.BASE_PATH}/${id}/suspend?reason=${encodeURIComponent(reason)}`
        : `${this.BASE_PATH}/${id}/suspend`
      
      const result = await apiClient.post<Organization>(endpoint)
      
      if (process.env.NODE_ENV === 'development') {
        console.log('✅ Organization suspended:', { id, reason })
      }
      
      return result
    } catch (error: any) {
      console.error('❌ Failed to suspend organization:', {
        id,
        reason,
        error: error.message,
      })
      throw new Error(`Failed to suspend organization ${id}: ${error.message}`)
    }
  }

  /**
   * Archive organization
   * Backend: POST /api/organizations/{id}/archive?reason={reason}
   * 
   * @param id - Organization ID
   * @param reason - Optional reason for archival
   * @throws {Error} If ID is invalid or request fails
   */
  async archive(id: string, reason?: string): Promise<Organization> {
    // Input validation
    if (!id || id.trim() === '') {
      throw new Error('Organization ID is required')
    }

    try {
      // Build URL with query parameter (not body)
      const endpoint = reason 
        ? `${this.BASE_PATH}/${id}/archive?reason=${encodeURIComponent(reason)}`
        : `${this.BASE_PATH}/${id}/archive`
      
      const result = await apiClient.post<Organization>(endpoint)
      
      if (process.env.NODE_ENV === 'development') {
        console.log('✅ Organization archived:', { id, reason })
      }
      
      return result
    } catch (error: any) {
      console.error('❌ Failed to archive organization:', {
        id,
        reason,
        error: error.message,
      })
      throw new Error(`Failed to archive organization ${id}: ${error.message}`)
    }
  }

  // ========== Query Operations ==========

  /**
   * Get organizations by status
   * Backend: GET /api/organizations/status/{status}
   * 
   * @param status - Organization status to filter by
   * @returns List of organizations with the specified status
   * @throws {Error} If status is invalid or request fails
   */
  async getByStatus(status: OrganizationStatus): Promise<Organization[]> {
    // Input validation
    if (!status) {
      throw new Error('Organization status is required')
    }

    // Validate status is a valid enum value
    const validStatuses: OrganizationStatus[] = ['DRAFT', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'ARCHIVED']
    if (!validStatuses.includes(status)) {
      throw new Error(`Invalid organization status. Must be one of: ${validStatuses.join(', ')}`)
    }

    try {
      return await apiClient.get<Organization[]>(`${this.BASE_PATH}/status/${status}`)
    } catch (error: any) {
      console.error('❌ Failed to fetch organizations by status:', { status, error: error.message })
      throw new Error(`Failed to fetch organizations with status ${status}: ${error.message}`)
    }
  }

  /**
   * Get active organizations only
   * Convenience method using getByStatus
   * 
   * @returns List of active organizations
   * @throws {Error} If request fails
   */
  async getActive(): Promise<Organization[]> {
    try {
      return await this.getByStatus('ACTIVE')
    } catch (error: any) {
      console.error('❌ Failed to fetch active organizations:', error.message)
      throw new Error(`Failed to fetch active organizations: ${error.message}`)
    }
  }

  /**
   * Get all organizations (unpaginated)
   * Fetches all by requesting large page size
   * 
   * @returns List of all organizations
   * @throws {Error} If request fails
   */
  async getAllUnpaged(): Promise<Organization[]> {
    try {
      const response = await this.getAll({ page: 0, size: 1000 })
      return response.content
    } catch (error: any) {
      console.error('❌ Failed to fetch all organizations:', error.message)
      throw new Error(`Failed to fetch all organizations: ${error.message}`)
    }
  }
}

export const organizationsAPI = new OrganizationsAPI()
