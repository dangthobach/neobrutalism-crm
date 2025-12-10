/**
 * Content Tag API Client
 */

import { apiClient, PageResponse } from './client'
import type {
  ContentTag,
  CreateTagRequest,
  UpdateTagRequest,
} from '@/types/content'

export const tagApi = {
  getAll: async (): Promise<PageResponse<ContentTag>> => {
    const response = await apiClient.get<PageResponse<ContentTag>>('/content-tags')
    return response as any
  },

  getById: async (id: string): Promise<ContentTag> => {
    const response = await apiClient.get<ContentTag>(`/content-tags/${id}`)
    return response as any
  },

  getBySlug: async (slug: string): Promise<ContentTag> => {
    const response = await apiClient.get<ContentTag>(`/content-tags/slug/${slug}`)
    return response as any
  },

  create: async (data: CreateTagRequest): Promise<ContentTag> => {
    const response = await apiClient.post<ContentTag>('/content-tags', data)
    return response as any
  },

  update: async (id: string, data: UpdateTagRequest): Promise<ContentTag> => {
    const response = await apiClient.put<ContentTag>(`/content-tags/${id}`, data)
    return response as any
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/content-tags/${id}`)
  },

  getPopular: async (limit?: number): Promise<ContentTag[]> => {
    const response = await apiClient.get<ContentTag[]>('/content-tags/popular', { limit: limit || 20 })
    return response as any
  },

  search: async (keyword: string): Promise<ContentTag[]> => {
    const response = await apiClient.get<ContentTag[]>('/content-tags/search', { keyword })
    return response as any
  },
}
