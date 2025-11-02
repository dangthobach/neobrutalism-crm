/**
 * React Query hooks for Branch API
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { branchApi } from '@/lib/api/branches'
import { toast } from 'sonner'
import type {
  CreateBranchRequest,
  UpdateBranchRequest,
  BranchSearchParams,
} from '@/types/branch'

/**
 * Get paginated branches
 */
export function useBranches(params?: BranchSearchParams) {
  return useQuery({
    queryKey: ['branches', params],
    queryFn: () => branchApi.getAll(params),
  })
}

/**
 * Get single branch by ID
 */
export function useBranch(id: string) {
  return useQuery({
    queryKey: ['branches', id],
    queryFn: () => branchApi.getById(id),
    enabled: !!id,
  })
}

/**
 * Get branch by code
 */
export function useBranchByCode(code: string) {
  return useQuery({
    queryKey: ['branches', 'code', code],
    queryFn: () => branchApi.getByCode(code),
    enabled: !!code,
  })
}

/**
 * Get branches by organization
 */
export function useBranchesByOrganization(organizationId: string, params?: BranchSearchParams) {
  return useQuery({
    queryKey: ['branches', 'organization', organizationId, params],
    queryFn: () => branchApi.getByOrganization(organizationId, params),
    enabled: !!organizationId,
  })
}

/**
 * Get active branches
 */
export function useActiveBranches(params?: BranchSearchParams) {
  return useQuery({
    queryKey: ['branches', 'active', params],
    queryFn: () => branchApi.getActive(params),
  })
}

/**
 * Search branches
 */
export function useBranchSearch(keyword: string) {
  return useQuery({
    queryKey: ['branches', 'search', keyword],
    queryFn: () => branchApi.search(keyword),
    enabled: keyword.length > 0,
  })
}

/**
 * Create branch
 */
export function useCreateBranch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateBranchRequest) => branchApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['branches'] })
      toast.success('Branch created successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to create branch', {
        description: error.message,
      })
    },
  })
}

/**
 * Update branch
 */
export function useUpdateBranch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateBranchRequest }) =>
      branchApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['branches'] })
      queryClient.invalidateQueries({ queryKey: ['branches', variables.id] })
      toast.success('Branch updated successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to update branch', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete branch
 */
export function useDeleteBranch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => branchApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['branches'] })
      toast.success('Branch deleted successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to delete branch', {
        description: error.message,
      })
    },
  })
}
