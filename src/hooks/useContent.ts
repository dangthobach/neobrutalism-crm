/**
 * Content Hooks - React Query hooks for content operations
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { contentApi } from '@/lib/api/content'
import type {
  Content,
  CreateContentRequest,
  UpdateContentRequest,
  ContentSearchParams,
  ContentStats,
} from '@/types/content'

// Query Keys
const CONTENT_KEYS = {
  all: ['content'] as const,
  lists: () => [...CONTENT_KEYS.all, 'list'] as const,
  list: (params: ContentSearchParams) => [...CONTENT_KEYS.lists(), params] as const,
  details: () => [...CONTENT_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...CONTENT_KEYS.details(), id] as const,
  slug: (slug: string) => [...CONTENT_KEYS.all, 'slug', slug] as const,
  author: (authorId: string) => [...CONTENT_KEYS.all, 'author', authorId] as const,
  category: (categoryId: string) => [...CONTENT_KEYS.all, 'category', categoryId] as const,
  tag: (tagId: string) => [...CONTENT_KEYS.all, 'tag', tagId] as const,
  series: (seriesId: string) => [...CONTENT_KEYS.all, 'series', seriesId] as const,
  published: () => [...CONTENT_KEYS.all, 'published'] as const,
  trending: () => [...CONTENT_KEYS.all, 'trending'] as const,
  stats: () => [...CONTENT_KEYS.all, 'stats'] as const,
}

// Queries
export const useContents = (params: ContentSearchParams = {}) => {
  return useQuery({
    queryKey: CONTENT_KEYS.list(params),
    queryFn: () => contentApi.getAll(params),
  })
}

export const useContent = (id: string, enabled = true) => {
  return useQuery({
    queryKey: CONTENT_KEYS.detail(id),
    queryFn: () => contentApi.getById(id),
    enabled: enabled && !!id,
  })
}

export const useContentBySlug = (slug: string, enabled = true) => {
  return useQuery({
    queryKey: CONTENT_KEYS.slug(slug),
    queryFn: () => contentApi.getBySlug(slug),
    enabled: enabled && !!slug,
  })
}

export const useContentByAuthor = (authorId: string, enabled = true) => {
  return useQuery({
    queryKey: CONTENT_KEYS.author(authorId),
    queryFn: () => contentApi.getByAuthor(authorId),
    enabled: enabled && !!authorId,
  })
}

export const useContentByCategory = (categoryId: string, enabled = true) => {
  return useQuery({
    queryKey: CONTENT_KEYS.category(categoryId),
    queryFn: () => contentApi.getByCategory(categoryId),
    enabled: enabled && !!categoryId,
  })
}

export const useContentByTag = (tagId: string, enabled = true) => {
  return useQuery({
    queryKey: CONTENT_KEYS.tag(tagId),
    queryFn: () => contentApi.getByTag(tagId),
    enabled: enabled && !!tagId,
  })
}

export const useContentBySeries = (seriesId: string, enabled = true) => {
  return useQuery({
    queryKey: CONTENT_KEYS.series(seriesId),
    queryFn: () => contentApi.getBySeries(seriesId),
    enabled: enabled && !!seriesId,
  })
}

export const usePublishedContent = () => {
  return useQuery({
    queryKey: CONTENT_KEYS.published(),
    queryFn: () => contentApi.getPublished(),
  })
}

export const useTrendingContent = (limit?: number) => {
  return useQuery({
    queryKey: [...CONTENT_KEYS.trending(), limit],
    queryFn: () => contentApi.getTrending(limit),
  })
}

export const useContentStats = () => {
  return useQuery({
    queryKey: CONTENT_KEYS.stats(),
    queryFn: () => contentApi.getStats(),
  })
}

// Mutations
export const useCreateContent = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: CreateContentRequest) => contentApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
      toast.success('Content created successfully')
    },
    onError: () => {
      toast.error('Failed to create content')
    },
  })
}

export const useUpdateContent = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateContentRequest }) =>
      contentApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
      toast.success('Content updated successfully')
    },
    onError: () => {
      toast.error('Failed to update content')
    },
  })
}

export const useDeleteContent = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => contentApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
      toast.success('Content deleted successfully')
    },
    onError: () => {
      toast.error('Failed to delete content')
    },
  })
}

export const usePublishContent = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => contentApi.publish(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.detail(id) })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.published() })
      toast.success('Content published successfully')
    },
    onError: () => {
      toast.error('Failed to publish content')
    },
  })
}

export const useUnpublishContent = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => contentApi.unpublish(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.detail(id) })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.published() })
      toast.success('Content unpublished successfully')
    },
    onError: () => {
      toast.error('Failed to unpublish content')
    },
  })
}

export const useArchiveContent = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => contentApi.archive(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.detail(id) })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
      toast.success('Content archived successfully')
    },
    onError: () => {
      toast.error('Failed to archive content')
    },
  })
}

export const useIncrementContentViews = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => contentApi.incrementViews(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.detail(id) })
      queryClient.invalidateQueries({ queryKey: CONTENT_KEYS.stats() })
    },
    onError: () => {
      // Silent fail for view tracking
      console.error('Failed to increment views')
    },
  })
}
