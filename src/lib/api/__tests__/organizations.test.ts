/**
 * Organizations API Tests
 * Tests for organizations.ts API service
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { organizationsAPI, Organization, OrganizationRequest } from '../organizations'
import { apiClient } from '../client'

// Mock apiClient
vi.mock('../client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
  ApiError: class ApiError extends Error {
    constructor(
      public status: number,
      public code: string,
      message: string,
      public data?: any
    ) {
      super(message)
      this.name = 'ApiError'
    }
  },
}))

describe('OrganizationsAPI', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mockOrganization: Organization = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    name: 'Test Organization',
    code: 'TEST-ORG',
    description: 'Test Description',
    email: 'test@example.com',
    phone: '+1234567890',
    website: 'https://test.com',
    address: '123 Test St',
    status: 'ACTIVE',
    deleted: false,
    createdAt: '2024-01-01T00:00:00Z',
    createdBy: 'system',
    updatedAt: '2024-01-01T00:00:00Z',
    updatedBy: 'system',
  }

  const mockOrganizationRequest: OrganizationRequest = {
    name: 'Test Organization',
    code: 'TEST-ORG',
    description: 'Test Description',
    email: 'test@example.com',
    phone: '+1234567890',
    website: 'https://test.com',
    address: '123 Test St',
  }

  describe('CRUD Operations', () => {
    it('should get all organizations with pagination', async () => {
      const mockResponse = {
        content: [mockOrganization],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true,
        first: true,
      }

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse)

      const result = await organizationsAPI.getAll({ page: 0, size: 20 })

      expect(apiClient.get).toHaveBeenCalledWith('/organizations', { page: 0, size: 20 })
      expect(result).toEqual(mockResponse)
    })

    it('should get organization by ID', async () => {
      vi.mocked(apiClient.get).mockResolvedValue(mockOrganization)

      const result = await organizationsAPI.getById(mockOrganization.id)

      expect(apiClient.get).toHaveBeenCalledWith(`/organizations/${mockOrganization.id}`)
      expect(result).toEqual(mockOrganization)
    })

    it('should get organization by code', async () => {
      vi.mocked(apiClient.get).mockResolvedValue(mockOrganization)

      const result = await organizationsAPI.getByCode(mockOrganization.code)

      expect(apiClient.get).toHaveBeenCalledWith(`/organizations/code/${mockOrganization.code}`)
      expect(result).toEqual(mockOrganization)
    })

    it('should create organization', async () => {
      vi.mocked(apiClient.post).mockResolvedValue(mockOrganization)

      const result = await organizationsAPI.create(mockOrganizationRequest)

      expect(apiClient.post).toHaveBeenCalledWith('/organizations', mockOrganizationRequest)
      expect(result).toEqual(mockOrganization)
    })

    it('should update organization', async () => {
      vi.mocked(apiClient.put).mockResolvedValue(mockOrganization)

      const result = await organizationsAPI.update(mockOrganization.id, mockOrganizationRequest)

      expect(apiClient.put).toHaveBeenCalledWith(
        `/organizations/${mockOrganization.id}`,
        mockOrganizationRequest
      )
      expect(result).toEqual(mockOrganization)
    })

    it('should delete organization', async () => {
      vi.mocked(apiClient.delete).mockResolvedValue(undefined)

      await organizationsAPI.delete(mockOrganization.id)

      expect(apiClient.delete).toHaveBeenCalledWith(`/organizations/${mockOrganization.id}`)
    })
  })

  describe('Status Operations', () => {
    it('should activate organization without reason', async () => {
      vi.mocked(apiClient.post).mockResolvedValue(mockOrganization)

      const result = await organizationsAPI.activate(mockOrganization.id)

      expect(apiClient.post).toHaveBeenCalledWith(
        `/organizations/${mockOrganization.id}/activate`,
        undefined
      )
      expect(result).toEqual(mockOrganization)
    })

    it('should activate organization with reason', async () => {
      vi.mocked(apiClient.post).mockResolvedValue(mockOrganization)
      const reason = 'Compliance approved'

      const result = await organizationsAPI.activate(mockOrganization.id, reason)

      expect(apiClient.post).toHaveBeenCalledWith(
        `/organizations/${mockOrganization.id}/activate`,
        { reason }
      )
      expect(result).toEqual(mockOrganization)
    })

    it('should suspend organization', async () => {
      vi.mocked(apiClient.post).mockResolvedValue(mockOrganization)
      const reason = 'Policy violation'

      const result = await organizationsAPI.suspend(mockOrganization.id, reason)

      expect(apiClient.post).toHaveBeenCalledWith(
        `/organizations/${mockOrganization.id}/suspend`,
        { reason }
      )
      expect(result).toEqual(mockOrganization)
    })

    it('should archive organization', async () => {
      vi.mocked(apiClient.post).mockResolvedValue(mockOrganization)
      const reason = 'Company closed'

      const result = await organizationsAPI.archive(mockOrganization.id, reason)

      expect(apiClient.post).toHaveBeenCalledWith(
        `/organizations/${mockOrganization.id}/archive`,
        { reason }
      )
      expect(result).toEqual(mockOrganization)
    })
  })

  describe('Query Operations', () => {
    it('should get organizations by status', async () => {
      vi.mocked(apiClient.get).mockResolvedValue([mockOrganization])

      const result = await organizationsAPI.getByStatus('ACTIVE')

      expect(apiClient.get).toHaveBeenCalledWith('/organizations/status/ACTIVE')
      expect(result).toEqual([mockOrganization])
    })

    it('should get active organizations', async () => {
      vi.mocked(apiClient.get).mockResolvedValue([mockOrganization])

      const result = await organizationsAPI.getActive()

      expect(apiClient.get).toHaveBeenCalledWith('/organizations/status/ACTIVE')
      expect(result).toEqual([mockOrganization])
    })

    it('should get all organizations unpaged', async () => {
      const mockResponse = {
        content: [mockOrganization],
        page: 0,
        size: 1000,
        totalElements: 1,
        totalPages: 1,
        last: true,
        first: true,
      }

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse)

      const result = await organizationsAPI.getAllUnpaged()

      expect(apiClient.get).toHaveBeenCalledWith('/organizations', { page: 0, size: 1000 })
      expect(result).toEqual([mockOrganization])
    })
  })

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const networkError = new Error('Network error')
      vi.mocked(apiClient.get).mockRejectedValue(networkError)

      await expect(organizationsAPI.getById(mockOrganization.id)).rejects.toThrow()
    })

    it('should handle API errors', async () => {
      const apiError = {
        status: 404,
        code: 'NOT_FOUND',
        message: 'Organization not found',
      }
      vi.mocked(apiClient.get).mockRejectedValue(apiError)

      await expect(organizationsAPI.getById(mockOrganization.id)).rejects.toThrow()
    })
  })

  describe('Input Validation', () => {
    it('should throw error when ID is empty', async () => {
      await expect(organizationsAPI.getById('')).rejects.toThrow('Organization ID is required')
    })

    it('should throw error when ID is invalid UUID', async () => {
      await expect(organizationsAPI.getById('invalid-id')).rejects.toThrow('Invalid organization ID format')
    })

    it('should throw error when creating without name', async () => {
      const invalidData = { ...mockOrganizationRequest, name: '' }
      await expect(organizationsAPI.create(invalidData)).rejects.toThrow('Organization name is required')
    })

    it('should throw error when creating without code', async () => {
      const invalidData = { ...mockOrganizationRequest, code: '' }
      await expect(organizationsAPI.create(invalidData)).rejects.toThrow('Organization code is required')
    })

    it('should throw error when code has invalid format', async () => {
      const invalidData = { ...mockOrganizationRequest, code: 'invalid code!' }
      await expect(organizationsAPI.create(invalidData)).rejects.toThrow('Organization code must contain only uppercase')
    })

    it('should throw error when status is invalid', async () => {
      await expect(organizationsAPI.getByStatus('INVALID' as any)).rejects.toThrow('Invalid organization status')
    })
  })
})
