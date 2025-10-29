/**
 * React Query hooks for API Endpoint management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  apiEndpointApi,
  ApiEndpoint,
  ApiEndpointQueryParams,
  CreateApiEndpointRequest,
  UpdateApiEndpointRequest,
  HttpMethod
} from '@/lib/api/api-endpoints'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const API_ENDPOINTS_QUERY_KEY = 'api-endpoints'

/**
 * Fetch API endpoints with pagination
 */
export function useApiEndpoints(params?: ApiEndpointQueryParams) {
  return useQuery({
    queryKey: [API_ENDPOINTS_QUERY_KEY, params],
    queryFn: () => apiEndpointApi.getApiEndpoints(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch API endpoint by ID
 */
export function useApiEndpoint(id: string) {
  return useQuery({
    queryKey: [API_ENDPOINTS_QUERY_KEY, id],
    queryFn: () => apiEndpointApi.getApiEndpointById(id),
    enabled: !!id,
  })
}

/**
 * Fetch API endpoints by HTTP method
 */
export function useApiEndpointsByMethod(method: HttpMethod) {
  return useQuery({
    queryKey: [API_ENDPOINTS_QUERY_KEY, 'method', method],
    queryFn: () => apiEndpointApi.getApiEndpointsByMethod(method),
    enabled: !!method,
  })
}

/**
 * Fetch API endpoints by tag
 */
export function useApiEndpointsByTag(tag: string) {
  return useQuery({
    queryKey: [API_ENDPOINTS_QUERY_KEY, 'tag', tag],
    queryFn: () => apiEndpointApi.getApiEndpointsByTag(tag),
    enabled: !!tag,
  })
}

/**
 * Fetch public API endpoints
 */
export function usePublicApiEndpoints() {
  return useQuery({
    queryKey: [API_ENDPOINTS_QUERY_KEY, 'public'],
    queryFn: () => apiEndpointApi.getPublicApiEndpoints(),
  })
}

/**
 * Fetch API endpoint by method and path
 */
export function useApiEndpointByMethodAndPath(method: HttpMethod, path: string) {
  return useQuery({
    queryKey: [API_ENDPOINTS_QUERY_KEY, 'find', method, path],
    queryFn: () => apiEndpointApi.getApiEndpointByMethodAndPath(method, path),
    enabled: !!method && !!path,
  })
}

/**
 * Create new API endpoint
 */
export function useCreateApiEndpoint() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateApiEndpointRequest) => apiEndpointApi.createApiEndpoint(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [API_ENDPOINTS_QUERY_KEY] })
      toast.success('API endpoint created successfully', {
        description: `${data.method} ${data.path} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create API endpoint', {
        description: error.message,
      })
    },
  })
}

/**
 * Update API endpoint
 */
export function useUpdateApiEndpoint() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateApiEndpointRequest }) =>
      apiEndpointApi.updateApiEndpoint(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [API_ENDPOINTS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [API_ENDPOINTS_QUERY_KEY, variables.id] })
      toast.success('API endpoint updated successfully', {
        description: `${data.method} ${data.path} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update API endpoint', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete API endpoint
 */
export function useDeleteApiEndpoint() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => apiEndpointApi.deleteApiEndpoint(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [API_ENDPOINTS_QUERY_KEY] })
      toast.success('API endpoint deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete API endpoint', {
        description: error.message,
      })
    },
  })
}

/**
 * Check if method + path combination exists
 */
export function useCheckMethodPath() {
  return useMutation({
    mutationFn: ({ method, path }: { method: HttpMethod; path: string }) =>
      apiEndpointApi.checkMethodPath(method, path),
  })
}
