/**
 * Content Series Hooks - React Query hooks for series operations
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { seriesApi } from '@/lib/api/content-series'
import type {
  ContentSeries,
  CreateSeriesRequest,
  UpdateSeriesRequest,
} from '@/types/content'

// Query Keys
const SERIES_KEYS = {
  all: ['content-series'] as const,
  lists: () => [...SERIES_KEYS.all, 'list'] as const,
  details: () => [...SERIES_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...SERIES_KEYS.details(), id] as const,
  slug: (slug: string) => [...SERIES_KEYS.all, 'slug', slug] as const,
  active: () => [...SERIES_KEYS.all, 'active'] as const,
  featured: () => [...SERIES_KEYS.all, 'featured'] as const,
}

// Queries
export const useSeries = () => {
  return useQuery({
    queryKey: SERIES_KEYS.lists(),
    queryFn: () => seriesApi.getAll(),
  })
}

export const useSeriesById = (id: string, enabled = true) => {
  return useQuery({
    queryKey: SERIES_KEYS.detail(id),
    queryFn: () => seriesApi.getById(id),
    enabled: enabled && !!id,
  })
}

export const useSeriesBySlug = (slug: string, enabled = true) => {
  return useQuery({
    queryKey: SERIES_KEYS.slug(slug),
    queryFn: () => seriesApi.getBySlug(slug),
    enabled: enabled && !!slug,
  })
}

export const useActiveSeries = () => {
  return useQuery({
    queryKey: SERIES_KEYS.active(),
    queryFn: () => seriesApi.getActive(),
  })
}

export const useFeaturedSeries = () => {
  return useQuery({
    queryKey: SERIES_KEYS.featured(),
    queryFn: () => seriesApi.getFeatured(),
  })
}

// Mutations
export const useCreateSeries = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: CreateSeriesRequest) => seriesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.active() })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.featured() })
      toast.success('Series created successfully')
    },
    onError: () => {
      toast.error('Failed to create series')
    },
  })
}

export const useUpdateSeries = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSeriesRequest }) =>
      seriesApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.active() })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.featured() })
      toast.success('Series updated successfully')
    },
    onError: () => {
      toast.error('Failed to update series')
    },
  })
}

export const useDeleteSeries = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => seriesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.active() })
      queryClient.invalidateQueries({ queryKey: SERIES_KEYS.featured() })
      toast.success('Series deleted successfully')
    },
    onError: () => {
      toast.error('Failed to delete series')
    },
  })
}
