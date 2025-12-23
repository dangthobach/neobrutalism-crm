/**
 * Customer React Query Hooks
 * Provides data fetching and mutations for customers
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { customerApi } from '@/lib/api/customers'
import type {
  Customer,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerSearchParams,
} from '@/types/customer'
import { toast } from 'sonner'

// Query keys
export const customerKeys = {
  all: ['customers'] as const,
  lists: () => [...customerKeys.all, 'list'] as const,
  list: (params?: CustomerSearchParams) => [...customerKeys.lists(), params] as const,
  details: () => [...customerKeys.all, 'detail'] as const,
  detail: (id: string) => [...customerKeys.details(), id] as const,
  byOwner: (ownerId: string) => [...customerKeys.all, 'owner', ownerId] as const,
  byBranch: (branchId: string) => [...customerKeys.all, 'branch', branchId] as const,
  vip: () => [...customerKeys.all, 'vip'] as const,
  stats: () => [...customerKeys.all, 'stats'] as const,
  search: (keyword: string) => [...customerKeys.all, 'search', keyword] as const,
}

/**
 * Get all customers with pagination
 */
export function useCustomers(params?: CustomerSearchParams) {
  return useQuery({
    queryKey: customerKeys.list(params),
    queryFn: () => customerApi.getAll(params),
  })
}

/**
 * Get customer by ID
 */
export function useCustomer(id: string | undefined) {
  return useQuery({
    queryKey: customerKeys.detail(id!),
    queryFn: () => customerApi.getById(id!),
    enabled: !!id,
  })
}

/**
 * Get customers by owner
 */
export function useCustomersByOwner(ownerId: string, params?: CustomerSearchParams) {
  return useQuery({
    queryKey: customerKeys.byOwner(ownerId),
    queryFn: () => customerApi.getByOwner(ownerId, params),
    enabled: !!ownerId,
  })
}

/**
 * Get customers by branch
 */
export function useCustomersByBranch(branchId: string, params?: CustomerSearchParams) {
  return useQuery({
    queryKey: customerKeys.byBranch(branchId),
    queryFn: () => customerApi.getByBranch(branchId, params),
    enabled: !!branchId,
  })
}

/**
 * Get VIP customers
 */
export function useVipCustomers(params?: CustomerSearchParams) {
  return useQuery({
    queryKey: customerKeys.vip(),
    queryFn: () => customerApi.getVipCustomers(params),
  })
}

/**
 * Get customer statistics
 */
export function useCustomerStats() {
  return useQuery({
    queryKey: customerKeys.stats(),
    queryFn: () => customerApi.getStats(),
  })
}

/**
 * Search customers
 */
export function useCustomerSearch(keyword: string) {
  return useQuery({
    queryKey: customerKeys.search(keyword),
    queryFn: () => customerApi.search(keyword),
    enabled: keyword.length > 0,
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
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer created successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer updated successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer deleted successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer converted to prospect')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to convert customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer activated successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to activate customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer deactivated')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to deactivate customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer reactivated')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to reactivate customer')
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
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: customerKeys.lists() })
      queryClient.invalidateQueries({ queryKey: customerKeys.stats() })
      toast.success('Customer blacklisted')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to blacklist customer')
    },
  })
}
