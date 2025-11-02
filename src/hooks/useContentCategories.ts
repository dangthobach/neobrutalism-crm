/**
 * Content Category Hooks - React Query hooks for category operations
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { categoryApi } from '@/lib/api/content-categories'
import type {
  ContentCategory,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@/types/content'

// Query Keys
const CATEGORY_KEYS = {
  all: ['content-categories'] as const,
  lists: () => [...CATEGORY_KEYS.all, 'list'] as const,
  details: () => [...CATEGORY_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...CATEGORY_KEYS.details(), id] as const,
  slug: (slug: string) => [...CATEGORY_KEYS.all, 'slug', slug] as const,
  tree: () => [...CATEGORY_KEYS.all, 'tree'] as const,
  active: () => [...CATEGORY_KEYS.all, 'active'] as const,
}

// Queries
export const useCategories = () => {
  return useQuery({
    queryKey: CATEGORY_KEYS.lists(),
    queryFn: () => categoryApi.getAll(),
  })
}

export const useCategory = (id: string, enabled = true) => {
  return useQuery({
    queryKey: CATEGORY_KEYS.detail(id),
    queryFn: () => categoryApi.getById(id),
    enabled: enabled && !!id,
  })
}

export const useCategoryBySlug = (slug: string, enabled = true) => {
  return useQuery({
    queryKey: CATEGORY_KEYS.slug(slug),
    queryFn: () => categoryApi.getBySlug(slug),
    enabled: enabled && !!slug,
  })
}

export const useCategoryTree = () => {
  return useQuery({
    queryKey: CATEGORY_KEYS.tree(),
    queryFn: () => categoryApi.getTree(),
  })
}

export const useActiveCategories = () => {
  return useQuery({
    queryKey: CATEGORY_KEYS.active(),
    queryFn: () => categoryApi.getActive(),
  })
}

// Mutations
export const useCreateCategory = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoryApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.tree() })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.active() })
      toast.success('Category created successfully')
    },
    onError: () => {
      toast.error('Failed to create category')
    },
  })
}

export const useUpdateCategory = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCategoryRequest }) =>
      categoryApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.tree() })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.active() })
      toast.success('Category updated successfully')
    },
    onError: () => {
      toast.error('Failed to update category')
    },
  })
}

export const useDeleteCategory = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) => categoryApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.lists() })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.tree() })
      queryClient.invalidateQueries({ queryKey: CATEGORY_KEYS.active() })
      toast.success('Category deleted successfully')
    },
    onError: () => {
      toast.error('Failed to delete category')
    },
  })
}
