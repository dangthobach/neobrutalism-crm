/**
 * React Query hooks for Customer API
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { customerApi } from '@/lib/api/customers'
import { toast } from 'sonner'
import type {
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerSearchParams,
} from '@/types/customer'

/**
 * Get paginated customers
 */
export function useCustomers(params?: CustomerSearchParams) {
  return useQuery({
    queryKey: ['customers', params],
    queryFn: () => customerApi.getAll(params),
  })
}

/**
 * Get single customer by ID
 */
export function useCustomer(id: string) {
  return useQuery({
    queryKey: ['customers', id],
    queryFn: () => customerApi.getById(id),
    enabled: !!id,
  })
}

/**
 * Search customers
 */
export function useCustomerSearch(keyword: string) {
  return useQuery({
    queryKey: ['customers', 'search', keyword],
    queryFn: () => customerApi.search(keyword),
    enabled: keyword.length > 0,
  })
}

/**
 * Get customers by owner
 */
export function useCustomersByOwner(ownerId: string, params?: CustomerSearchParams) {
  return useQuery({
    queryKey: ['customers', 'owner', ownerId, params],
    queryFn: () => customerApi.getByOwner(ownerId, params),
    enabled: !!ownerId,
  })
}

/**
 * Get customers by branch
 */
export function useCustomersByBranch(branchId: string, params?: CustomerSearchParams) {
  return useQuery({
    queryKey: ['customers', 'branch', branchId, params],
    queryFn: () => customerApi.getByBranch(branchId, params),
    enabled: !!branchId,
  })
}

/**
 * Get VIP customers
 */
export function useVipCustomers(params?: CustomerSearchParams) {
  return useQuery({
    queryKey: ['customers', 'vip', params],
    queryFn: () => customerApi.getVipCustomers(params),
  })
}

/**
 * Get customer statistics
 */
export function useCustomerStats() {
  return useQuery({
    queryKey: ['customers', 'stats'],
    queryFn: () => customerApi.getStats(),
  })
}

/**
 * Create customer
 */
export function useCreateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateCustomerRequest) => customerApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success('Customer created successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to create customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Update customer
 */
export function useUpdateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerRequest }) =>
      customerApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer updated successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to update customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete customer
 */
export function useDeleteCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => customerApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success('Customer deleted successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to delete customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Convert customer to prospect
 */
export function useConvertToProspect() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      customerApi.convertToProspect(id, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer converted to prospect')
    },
    onError: (error: any) => {
      toast.error('Failed to convert customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Convert customer to active
 */
export function useConvertToActive() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      customerApi.convertToActive(id, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer converted to active')
    },
    onError: (error: any) => {
      toast.error('Failed to convert customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Deactivate customer
 */
export function useDeactivateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      customerApi.deactivate(id, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer deactivated')
    },
    onError: (error: any) => {
      toast.error('Failed to deactivate customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Reactivate customer
 */
export function useReactivateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      customerApi.reactivate(id, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer reactivated')
    },
    onError: (error: any) => {
      toast.error('Failed to reactivate customer', {
        description: error.message,
      })
    },
  })
}

/**
 * Blacklist customer
 */
export function useBlacklistCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      customerApi.blacklist(id, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer blacklisted')
    },
    onError: (error: any) => {
      toast.error('Failed to blacklist customer', {
        description: error.message,
      })
    },
  })
}
