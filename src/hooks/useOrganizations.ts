/**
 * React Query hooks for Organization management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { 
  organizationsAPI, 
  Organization, 
  OrganizationRequest,
  OrganizationQueryParams,
  OrganizationStatus 
} from '@/lib/api/organizations'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const ORGANIZATIONS_QUERY_KEY = 'organizations'

/**
 * Fetch organizations with pagination
 */
export function useOrganizations(params?: OrganizationQueryParams) {
  return useQuery({
    queryKey: [ORGANIZATIONS_QUERY_KEY, params],
    queryFn: () => organizationsAPI.getAll(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch all organizations without pagination
 */
export function useAllOrganizations() {
  return useQuery({
    queryKey: [ORGANIZATIONS_QUERY_KEY, 'all'],
    queryFn: () => organizationsAPI.getAllUnpaged(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch organization by ID
 */
export function useOrganization(id: string) {
  return useQuery({
    queryKey: [ORGANIZATIONS_QUERY_KEY, id],
    queryFn: () => organizationsAPI.getById(id),
    enabled: !!id,
  })
}

/**
 * Fetch organization by code
 */
export function useOrganizationByCode(code: string) {
  return useQuery({
    queryKey: [ORGANIZATIONS_QUERY_KEY, 'code', code],
    queryFn: () => organizationsAPI.getByCode(code),
    enabled: !!code,
  })
}

/**
 * Fetch organizations by status
 */
export function useOrganizationsByStatus(status: OrganizationStatus) {
  return useQuery({
    queryKey: [ORGANIZATIONS_QUERY_KEY, 'status', status],
    queryFn: () => organizationsAPI.getByStatus(status),
  })
}

/**
 * Fetch active organizations only
 */
export function useActiveOrganizations() {
  return useQuery({
    queryKey: [ORGANIZATIONS_QUERY_KEY, 'active'],
    queryFn: () => organizationsAPI.getActive(),
  })
}

/**
 * Create new organization
 */
export function useCreateOrganization() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: OrganizationRequest) => organizationsAPI.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ORGANIZATIONS_QUERY_KEY] })
      toast.success('Organization created successfully', {
        description: `${data.name} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create organization', {
        description: error.message || 'An unexpected error occurred',
      })
    },
  })
}

/**
 * Update organization
 */
export function useUpdateOrganization() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: OrganizationRequest }) =>
      organizationsAPI.update(id, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ORGANIZATIONS_QUERY_KEY] })
      toast.success('Organization updated successfully', {
        description: `${data.name} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update organization', {
        description: error.message || 'An unexpected error occurred',
      })
    },
  })
}

/**
 * Delete organization
 */
export function useDeleteOrganization() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => organizationsAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ORGANIZATIONS_QUERY_KEY] })
      toast.success('Organization deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete organization', {
        description: error.message || 'An unexpected error occurred',
      })
    },
  })
}

/**
 * Activate organization
 */
export function useActivateOrganization() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      organizationsAPI.activate(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ORGANIZATIONS_QUERY_KEY] })
      toast.success('Organization activated successfully', {
        description: `${data.name} is now active.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to activate organization', {
        description: error.message || 'An unexpected error occurred',
      })
    },
  })
}

/**
 * Suspend organization
 */
export function useSuspendOrganization() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      organizationsAPI.suspend(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ORGANIZATIONS_QUERY_KEY] })
      toast.success('Organization suspended successfully', {
        description: `${data.name} has been suspended.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to suspend organization', {
        description: error.message || 'An unexpected error occurred',
      })
    },
  })
}

/**
 * Archive organization
 */
export function useArchiveOrganization() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      organizationsAPI.archive(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [ORGANIZATIONS_QUERY_KEY] })
      toast.success('Organization archived successfully', {
        description: `${data.name} has been archived.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to archive organization', {
        description: error.message || 'An unexpected error occurred',
      })
    },
  })
}
