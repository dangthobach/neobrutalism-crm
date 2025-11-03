/**
 * Contact React Query Hooks
 * Provides data fetching and mutations for contacts
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { contactApi } from '@/lib/api/contacts'
import type {
  Contact,
  CreateContactRequest,
  UpdateContactRequest,
  ContactSearchParams,
} from '@/types/contact'
import { toast } from 'sonner'

// Query keys
export const contactKeys = {
  all: ['contacts'] as const,
  lists: () => [...contactKeys.all, 'list'] as const,
  list: (params?: ContactSearchParams) => [...contactKeys.lists(), params] as const,
  details: () => [...contactKeys.all, 'detail'] as const,
  detail: (id: string) => [...contactKeys.details(), id] as const,
  byCustomer: (customerId: string) => [...contactKeys.all, 'customer', customerId] as const,
  primaryByCustomer: (customerId: string) => [...contactKeys.all, 'customer', customerId, 'primary'] as const,
  search: (keyword: string) => [...contactKeys.all, 'search', keyword] as const,
}

/**
 * Get all contacts with pagination
 */
export function useContacts(params?: ContactSearchParams) {
  return useQuery({
    queryKey: contactKeys.list(params),
    queryFn: () => contactApi.getAll(params),
  })
}

/**
 * Get contact by ID
 */
export function useContact(id: string | undefined) {
  return useQuery({
    queryKey: contactKeys.detail(id!),
    queryFn: () => contactApi.getById(id!),
    enabled: !!id,
  })
}

/**
 * Get contacts by customer
 */
export function useContactsByCustomer(customerId: string, params?: ContactSearchParams) {
  return useQuery({
    queryKey: contactKeys.byCustomer(customerId),
    queryFn: () => contactApi.getByCustomer(customerId, params),
    enabled: !!customerId,
  })
}

/**
 * Get primary contact for customer
 */
export function usePrimaryContact(customerId: string) {
  return useQuery({
    queryKey: contactKeys.primaryByCustomer(customerId),
    queryFn: () => contactApi.getPrimaryByCustomer(customerId),
    enabled: !!customerId,
  })
}

/**
 * Search contacts
 */
export function useContactSearch(keyword: string) {
  return useQuery({
    queryKey: contactKeys.search(keyword),
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
      queryClient.invalidateQueries({ queryKey: contactKeys.lists() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: contactKeys.byCustomer(data.customerId) })
      }
      toast.success('Contact created successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create contact')
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
      queryClient.invalidateQueries({ queryKey: contactKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: contactKeys.lists() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: contactKeys.byCustomer(data.customerId) })
      }
      toast.success('Contact updated successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update contact')
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
      queryClient.invalidateQueries({ queryKey: contactKeys.lists() })
      queryClient.invalidateQueries({ queryKey: contactKeys.all })
      toast.success('Contact deleted successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete contact')
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
      queryClient.invalidateQueries({ queryKey: contactKeys.detail(data.id) })
      queryClient.invalidateQueries({ queryKey: contactKeys.lists() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: contactKeys.byCustomer(data.customerId) })
        queryClient.invalidateQueries({ queryKey: contactKeys.primaryByCustomer(data.customerId) })
      }
      toast.success('Primary contact updated')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to set primary contact')
    },
  })
}
