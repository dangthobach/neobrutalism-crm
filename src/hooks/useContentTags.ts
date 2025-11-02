/**
 * Content Tag Hooks - React Query hooks for tag operations
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { tagApi } from '@/lib/api/content-tags'
import type {
  ContentTag,
  CreateTagRequest,
  UpdateTagRequest,
} from '@/types/content'

// Query Keys
const TAG_KEYS = {
  all: ['content-tags'] as const,
  lists: () => [...TAG_KEYS.all, 'list'] as const,
  details: () => [...TAG_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...TAG_KEYS.details(), id] as const,
  slug: (slug: string) => [...TAG_KEYS.all, 'slug', slug] as const,
  popular: (limit: number) => [...TAG_KEYS.all, 'popular', limit] as const,
  search: (keyword: string) => [...TAG_KEYS.all, 'search', keyword] as const,
}

// Queries
export const useTags = () => {
  return useQuery({
    queryKey: TAG_KEYS.lists(),
    queryFn: () => tagApi.getAll(),
  })
}

export const useTag = (id: string, enabled = true) => {
  return useQuery({
    queryKey: TAG_KEYS.detail(id),
    queryFn: () => tagApi.getById(id),
    enabled: enabled && !!id,
  })
}

export const useTagBySlug = (slug: string, enabled = true) => {
  return useQuery({
    queryKey: TAG_KEYS.slug(slug),
    queryFn: () => tagApi.getBySlug(slug),
    enabled: enabled && !!slug,
  })
}

export const usePopularTags = (limit = 20) => {
  return useQuery({
    queryKey: TAG_KEYS.popular(limit),
    queryFn: () => tagApi.getPopular(limit),
  })
}

export const useSearchTags = (keyword: string, enabled = true) => {
  return useQuery({
    queryKey: TAG_KEYS.search(keyword),
    queryFn: () => tagApi.search(keyword),
    enabled: enabled && keyword.length > 0,
  })
}

// Mutations
export const useCreateTag = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: CreateTagRequest) => tagApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TAG_KEYS.lists() })
      toast.success('Tag created successfully')
    },
    onError: () => {
      toast.error('Failed to create tag')
    },
  })
}

export const useUpdateTag = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTagRequest }) =>
      tagApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: TAG_KEYS.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: TAG_KEYS.lists() })
      toast.success('Tag updated successfully')
    },
    onError: () => {
      toast.error('Failed to update tag')
    },
  })
}

export const useDeleteTag = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => tagApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TAG_KEYS.lists() })
      toast.success('Tag deleted successfully')
    },
    onError: () => {
      toast.error('Failed to delete tag')
    },
  })
}
