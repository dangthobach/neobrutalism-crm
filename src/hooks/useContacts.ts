/**
 * React Query hooks for Contact API
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { contactApi } from '@/lib/api/contacts'
import { toast } from 'sonner'
import type {
  CreateContactRequest,
  UpdateContactRequest,
  ContactSearchParams,
} from '@/types/contact'

/**
 * Get paginated contacts
 */
export function useContacts(params?: ContactSearchParams) {
  return useQuery({
    queryKey: ['contacts', params],
    queryFn: () => contactApi.getAll(params),
  })
}

/**
 * Get single contact by ID
 */
export function useContact(id: string) {
  return useQuery({
    queryKey: ['contacts', id],
    queryFn: () => contactApi.getById(id),
    enabled: !!id,
  })
}

/**
 * Get contacts by customer
 */
export function useContactsByCustomer(customerId: string, params?: ContactSearchParams) {
  return useQuery({
    queryKey: ['contacts', 'customer', customerId, params],
    queryFn: () => contactApi.getByCustomer(customerId, params),
    enabled: !!customerId,
  })
}

/**
 * Get primary contact for customer
 */
export function usePrimaryContact(customerId: string) {
  return useQuery({
    queryKey: ['contacts', 'customer', customerId, 'primary'],
    queryFn: () => contactApi.getPrimaryByCustomer(customerId),
    enabled: !!customerId,
  })
}

/**
 * Search contacts
 */
export function useContactSearch(keyword: string) {
  return useQuery({
    queryKey: ['contacts', 'search', keyword],
    queryFn: () => contactApi.search(keyword),
    enabled: keyword.length > 0,
  })
}

/**
 * Create contact
 */
export function useCreateContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateContactRequest) => contactApi.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: ['contacts', 'customer', data.customerId] })
      }
      toast.success('Contact created successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to create contact', {
        description: error.message,
      })
    },
  })
}

/**
 * Update contact
 */
export function useUpdateContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateContactRequest }) =>
      contactApi.update(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      queryClient.invalidateQueries({ queryKey: ['contacts', variables.id] })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: ['contacts', 'customer', data.customerId] })
      }
      toast.success('Contact updated successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to update contact', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete contact
 */
export function useDeleteContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => contactApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      toast.success('Contact deleted successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to delete contact', {
        description: error.message,
      })
    },
  })
}

/**
 * Set contact as primary
 */
export function useSetPrimaryContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => contactApi.setPrimary(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: ['contacts', 'customer', data.customerId] })
      }
      toast.success('Contact set as primary')
    },
    onError: (error: any) => {
      toast.error('Failed to set primary contact', {
        description: error.message,
      })
    },
  })
}
