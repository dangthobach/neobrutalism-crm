/**
 * Content API Client
 * Handles all content-related API calls
 */

import { apiClient, PageResponse } from './client'
import type {
  Content,
  CreateContentRequest,
  UpdateContentRequest,
  ContentSearchParams,
  ContentStats,
} from '@/types/content'

export const contentApi = {
  /**
   * Get paginated list of content
   */
  getAll: async (params?: ContentSearchParams): Promise<PageResponse<Content>> => {
    const response = await apiClient.get<PageResponse<Content>>('/content', params)
    return response.data!
  },

  /**
   * Get content by ID
   */
  getById: async (id: string): Promise<Content> => {
    const response = await apiClient.get<Content>(`/content/${id}`)
    return response.data!
  },

  /**
   * Get content by slug
   */
  getBySlug: async (slug: string): Promise<Content> => {
    const response = await apiClient.get<Content>(`/content/slug/${slug}`)
    return response.data!
  },

  /**
   * Create new content
   */
  create: async (data: CreateContentRequest): Promise<Content> => {
    const response = await apiClient.post<Content>('/content', data)
    return response.data!
  },

  /**
   * Update content
   */
  update: async (id: string, data: UpdateContentRequest): Promise<Content> => {
    const response = await apiClient.put<Content>(`/content/${id}`, data)
    return response.data!
  },

  /**
   * Delete content
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/content/${id}`)
  },

  /**
   * Search content by keyword
   */
  search: async (keyword: string): Promise<Content[]> => {
    const response = await apiClient.get<Content[]>('/content/search', { keyword })
    return response.data!
  },

  /**
   * Get content by author
   */
  getByAuthor: async (authorId: string, params?: ContentSearchParams): Promise<PageResponse<Content>> => {
    const response = await apiClient.get<PageResponse<Content>>(`/content/author/${authorId}`, params)
    return response.data!
  },

  /**
   * Get content by category
   */
  getByCategory: async (categoryId: string, params?: ContentSearchParams): Promise<PageResponse<Content>> => {
    const response = await apiClient.get<PageResponse<Content>>(`/content/category/${categoryId}`, params)
    return response.data!
  },

  /**
   * Get content by tag
   */
  getByTag: async (tagId: string, params?: ContentSearchParams): Promise<PageResponse<Content>> => {
    const response = await apiClient.get<PageResponse<Content>>(`/content/tag/${tagId}`, params)
    return response.data!
  },

  /**
   * Get content by series
   */
  getBySeries: async (seriesId: string, params?: ContentSearchParams): Promise<PageResponse<Content>> => {
    const response = await apiClient.get<PageResponse<Content>>(`/content/series/${seriesId}`, params)
    return response.data!
  },

  /**
   * Get published content (public)
   */
  getPublished: async (params?: ContentSearchParams): Promise<PageResponse<Content>> => {
    const response = await apiClient.get<PageResponse<Content>>('/content/published', params)
    return response.data!
  },

  /**
   * Get trending content
   */
  getTrending: async (limit?: number): Promise<Content[]> => {
    const response = await apiClient.get<Content[]>('/content/trending', { limit: limit || 10 })
    return response.data!
  },

  /**
   * Increment view count
   */
  incrementViews: async (id: string): Promise<void> => {
    await apiClient.post(`/content/${id}/view`)
  },

  /**
   * Publish content
   */
  publish: async (id: string): Promise<Content> => {
    const response = await apiClient.post<Content>(`/content/${id}/publish`)
    return response.data!
  },

  /**
   * Unpublish content
   */
  unpublish: async (id: string): Promise<Content> => {
    const response = await apiClient.post<Content>(`/content/${id}/unpublish`)
    return response.data!
  },

  /**
   * Archive content
   */
  archive: async (id: string): Promise<Content> => {
    const response = await apiClient.post<Content>(`/content/${id}/archive`)
    return response.data!
  },

  /**
   * Get content statistics
   */
  getStats: async (): Promise<ContentStats> => {
    const response = await apiClient.get<ContentStats>('/content/stats')
    return response.data!
  },
}
