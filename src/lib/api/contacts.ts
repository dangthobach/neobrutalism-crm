/**
 * Contact API Client
 * Handles all contact-related API calls
 */

import { apiClient, PageResponse } from './client'
import type {
  Contact,
  CreateContactRequest,
  UpdateContactRequest,
  ContactSearchParams,
} from '@/types/contact'

export const contactApi = {
  /**
   * Get paginated list of contacts
   */
  getAll: async (params?: ContactSearchParams): Promise<PageResponse<Contact>> => {
    const response = await apiClient.get<PageResponse<Contact>>('/contacts', params)
    return response as any
  },

  /**
   * Get contact by ID
   */
  getById: async (id: string): Promise<Contact> => {
    const response = await apiClient.get<Contact>(`/contacts/${id}`)
    return response as any
  },

  /**
   * Create new contact
   */
  create: async (data: CreateContactRequest): Promise<Contact> => {
    const response = await apiClient.post<Contact>('/contacts', data)
    return response as any
  },

  /**
   * Update contact
   */
  update: async (id: string, data: UpdateContactRequest): Promise<Contact> => {
    const response = await apiClient.put<Contact>(`/contacts/${id}`, data)
    return response as any
  },

  /**
   * Delete contact
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/contacts/${id}`)
  },

  /**
   * Get contacts by customer
   */
  getByCustomer: async (customerId: string, params?: ContactSearchParams): Promise<PageResponse<Contact>> => {
    const response = await apiClient.get<PageResponse<Contact>>(`/contacts/customer/${customerId}`, params)
    return response as any
  },

  /**
   * Get primary contact for customer
   */
  getPrimaryByCustomer: async (customerId: string): Promise<Contact | null> => {
    try {
      const response = await apiClient.get<Contact>(`/contacts/customer/${customerId}/primary`)
      return response as any
    } catch (error) {
      return null
    }
  },

  /**
   * Set contact as primary
   */
  setPrimary: async (id: string): Promise<Contact> => {
    const response = await apiClient.post<Contact>(`/contacts/${id}/set-primary`)
    return response as any
  },

  /**
   * Search contacts by keyword
   */
  search: async (keyword: string): Promise<Contact[]> => {
    const response = await apiClient.get<Contact[]>('/contacts/search', { keyword })
    return response as any
  },
}
