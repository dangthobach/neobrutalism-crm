/**
 * Content Series API Client
 */

import { apiClient, PageResponse } from './client'
import type {
  ContentSeries,
  CreateSeriesRequest,
  UpdateSeriesRequest,
} from '@/types/content'

export const seriesApi = {
  getAll: async (): Promise<PageResponse<ContentSeries>> => {
    const response = await apiClient.get<PageResponse<ContentSeries>>('/content-series')
    return response as any
  },

  getById: async (id: string): Promise<ContentSeries> => {
    const response = await apiClient.get<ContentSeries>(`/content-series/${id}`)
    return response as any
  },

  getBySlug: async (slug: string): Promise<ContentSeries> => {
    const response = await apiClient.get<ContentSeries>(`/content-series/slug/${slug}`)
    return response as any
  },

  create: async (data: CreateSeriesRequest): Promise<ContentSeries> => {
    const response = await apiClient.post<ContentSeries>('/content-series', data)
    return response as any
  },

  update: async (id: string, data: UpdateSeriesRequest): Promise<ContentSeries> => {
    const response = await apiClient.put<ContentSeries>(`/content-series/${id}`, data)
    return response as any
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/content-series/${id}`)
  },

  getActive: async (): Promise<ContentSeries[]> => {
    const response = await apiClient.get<ContentSeries[]>('/content-series/active')
    return response as any
  },

  getFeatured: async (): Promise<ContentSeries[]> => {
    const response = await apiClient.get<ContentSeries[]>('/content-series/featured')
    return response as any
  },
}
