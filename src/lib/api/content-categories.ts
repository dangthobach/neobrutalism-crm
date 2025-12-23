/**
 * Content Category API Client
 */

import { apiClient, PageResponse } from './client'
import type {
  ContentCategory,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@/types/content'

export const categoryApi = {
  getAll: async (): Promise<PageResponse<ContentCategory>> => {
    const response = await apiClient.get<PageResponse<ContentCategory>>('/content-categories')
    return response as any
  },

  getById: async (id: string): Promise<ContentCategory> => {
    const response = await apiClient.get<ContentCategory>(`/content-categories/${id}`)
    return response as any
  },

  getBySlug: async (slug: string): Promise<ContentCategory> => {
    const response = await apiClient.get<ContentCategory>(`/content-categories/slug/${slug}`)
    return response as any
  },

  create: async (data: CreateCategoryRequest): Promise<ContentCategory> => {
    const response = await apiClient.post<ContentCategory>('/content-categories', data)
    return response as any
  },

  update: async (id: string, data: UpdateCategoryRequest): Promise<ContentCategory> => {
    const response = await apiClient.put<ContentCategory>(`/content-categories/${id}`, data)
    return response as any
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/content-categories/${id}`)
  },

  getTree: async (): Promise<ContentCategory[]> => {
    const response = await apiClient.get<ContentCategory[]>('/content-categories/tree')
    return response as any
  },

  getActive: async (): Promise<ContentCategory[]> => {
    const response = await apiClient.get<ContentCategory[]>('/content-categories/active')
    return response as any
  },
}
