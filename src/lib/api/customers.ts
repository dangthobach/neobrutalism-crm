/**
 * Customer API Client
 * Handles all customer-related API calls
 */

import { apiClient, PageResponse } from './client'
import type {
  Customer,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerSearchParams,
  CustomerStats,
} from '@/types/customer'

export const customerApi = {
  /**
   * Get paginated list of customers
   */
  getAll: async (params?: CustomerSearchParams): Promise<PageResponse<Customer>> => {
    const response = await apiClient.get<PageResponse<Customer>>('/customers', params)
    return response as any
  },

  /**
   * Get customer by ID
   */
  getById: async (id: string): Promise<Customer> => {
    const response = await apiClient.get<Customer>(`/customers/${id}`)
    return response as any
  },

  /**
   * Create new customer
   */
  create: async (data: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<Customer>('/customers', data)
    return response as any
  },

  /**
   * Update customer
   */
  update: async (id: string, data: UpdateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.put<Customer>(`/customers/${id}`, data)
    return response as any
  },

  /**
   * Delete customer
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/customers/${id}`)
  },

  /**
   * Search customers by keyword
   */
  search: async (keyword: string): Promise<Customer[]> => {
    const response = await apiClient.get<Customer[]>('/customers/search', { keyword })
    return response as any
  },

  /**
   * Get customers by owner
   */
  getByOwner: async (ownerId: string, params?: CustomerSearchParams): Promise<PageResponse<Customer>> => {
    const response = await apiClient.get<PageResponse<Customer>>(`/customers/owner/${ownerId}`, params)
    return response as any
  },

  /**
   * Get customers by branch
   */
  getByBranch: async (branchId: string, params?: CustomerSearchParams): Promise<PageResponse<Customer>> => {
    const response = await apiClient.get<PageResponse<Customer>>(`/customers/branch/${branchId}`, params)
    return response as any
  },

  /**
   * Get VIP customers
   */
  getVipCustomers: async (params?: CustomerSearchParams): Promise<PageResponse<Customer>> => {
    const response = await apiClient.get<PageResponse<Customer>>('/customers/vip', params)
    return response as any
  },

  /**
   * Convert customer to prospect
   */
  convertToProspect: async (id: string, reason?: string): Promise<Customer> => {
    const endpoint = reason 
      ? `/customers/${id}/convert-to-prospect?reason=${encodeURIComponent(reason)}`
      : `/customers/${id}/convert-to-prospect`
    const response = await apiClient.post<Customer>(endpoint)
    return response as any
  },

  /**
   * Convert customer to active
   */
  convertToActive: async (id: string, reason?: string): Promise<Customer> => {
    const endpoint = reason 
      ? `/customers/${id}/convert-to-active?reason=${encodeURIComponent(reason)}`
      : `/customers/${id}/convert-to-active`
    const response = await apiClient.post<Customer>(endpoint)
    return response as any
  },

  /**
   * Deactivate customer
   */
  deactivate: async (id: string, reason?: string): Promise<Customer> => {
    const endpoint = reason 
      ? `/customers/${id}/deactivate?reason=${encodeURIComponent(reason)}`
      : `/customers/${id}/deactivate`
    const response = await apiClient.post<Customer>(endpoint)
    return response as any
  },

  /**
   * Reactivate customer
   */
  reactivate: async (id: string, reason?: string): Promise<Customer> => {
    const endpoint = reason 
      ? `/customers/${id}/reactivate?reason=${encodeURIComponent(reason)}`
      : `/customers/${id}/reactivate`
    const response = await apiClient.post<Customer>(endpoint)
    return response as any
  },

  /**
   * Blacklist customer
   */
  blacklist: async (id: string, reason?: string): Promise<Customer> => {
    const endpoint = reason 
      ? `/customers/${id}/blacklist?reason=${encodeURIComponent(reason)}`
      : `/customers/${id}/blacklist`
    const response = await apiClient.post<Customer>(endpoint)
    return response as any
  },

  /**
   * Get customer statistics
   */
  getStats: async (): Promise<CustomerStats> => {
    const response = await apiClient.get<CustomerStats>('/customers/stats')
    return response as any
  },
}
