import { apiClient } from './client'
import type {
  Course,
  CreateCourseRequest,
  UpdateCourseRequest,
  CourseStatsResponse,
  PageResponse,
  CourseFilters,
} from '@/types/course'

const BASE_URL = '/api/courses'

// =====================================================
// CRUD Operations
// =====================================================

export const createCourse = async (data: CreateCourseRequest): Promise<Course> => {
  const response = await apiClient.post<Course>(BASE_URL, data)
  return response as any
}

export const updateCourse = async (id: string, data: UpdateCourseRequest): Promise<Course> => {
  const response = await apiClient.put<Course>(`${BASE_URL}/${id}`, data)
  return response as any
}

export const deleteCourse = async (id: string): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/${id}`)
}

export const getCourseById = async (id: string): Promise<Course> => {
  const response = await apiClient.get<Course>(`${BASE_URL}/${id}`)
  return response as any
}

// =====================================================
// Query Operations
// =====================================================

export const getAllCourses = async (
  page: number = 0,
  size: number = 20,
  filters?: CourseFilters
): Promise<PageResponse<Course>> => {
  const params = new URLSearchParams()
  params.append('page', page.toString())
  params.append('size', size.toString())
  
  if (filters) {
    if (filters.level) params.append('level', filters.level)
    if (filters.status) params.append('status', filters.status)
    if (filters.categoryId) params.append('categoryId', filters.categoryId)
    if (filters.instructorId) params.append('instructorId', filters.instructorId)
    if (filters.minPrice !== undefined) params.append('minPrice', filters.minPrice.toString())
    if (filters.maxPrice !== undefined) params.append('maxPrice', filters.maxPrice.toString())
    if (filters.language) params.append('language', filters.language)
    if (filters.keyword) params.append('keyword', filters.keyword)
    if (filters.tags?.length) params.append('tags', filters.tags.join(','))
  }

  const response = await apiClient.get<PageResponse<Course>>(`${BASE_URL}?${params}`)
  return response as any
}

export const getPublishedCourses = async (
  page: number = 0,
  size: number = 20
): Promise<PageResponse<Course>> => {
  const response = await apiClient.get<PageResponse<Course>>(
    `${BASE_URL}/published?page=${page}&size=${size}`
  )
  return response as any
}

export const getCourseBySlug = async (slug: string): Promise<Course> => {
  const response = await apiClient.get<Course>(`${BASE_URL}/slug/${slug}`)
  return response as any
}

export const getCoursesByInstructor = async (
  instructorId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<Course>> => {
  const response = await apiClient.get<PageResponse<Course>>(
    `${BASE_URL}/instructor/${instructorId}?page=${page}&size=${size}`
  )
  return response as any
}

export const getCoursesByCategory = async (
  categoryId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<Course>> => {
  const response = await apiClient.get<PageResponse<Course>>(
    `${BASE_URL}/category/${categoryId}?page=${page}&size=${size}`
  )
  return response as any
}

export const getFeaturedCourses = async (limit: number = 6): Promise<Course[]> => {
  const response = await apiClient.get<Course[]>(`${BASE_URL}/featured?limit=${limit}`)
  return response as any
}

export const getPopularCourses = async (limit: number = 6): Promise<Course[]> => {
  const response = await apiClient.get<Course[]>(`${BASE_URL}/popular?limit=${limit}`)
  return response as any
}

export const searchCourses = async (
  keyword: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<Course>> => {
  const response = await apiClient.get<PageResponse<Course>>(
    `${BASE_URL}/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`
  )
  return response as any
}

// =====================================================
// Action Operations
// =====================================================

export const publishCourse = async (id: string): Promise<Course> => {
  const response = await apiClient.post<Course>(`${BASE_URL}/${id}/publish`)
  return response as any
}

export const unpublishCourse = async (id: string): Promise<Course> => {
  const response = await apiClient.post<Course>(`${BASE_URL}/${id}/unpublish`)
  return response as any
}

export const archiveCourse = async (id: string): Promise<Course> => {
  const response = await apiClient.post<Course>(`${BASE_URL}/${id}/archive`)
  return response as any
}

export const duplicateCourse = async (id: string): Promise<Course> => {
  const response = await apiClient.post<Course>(`${BASE_URL}/${id}/duplicate`)
  return response as any
}

// =====================================================
// Statistics
// =====================================================

export const getCourseStats = async (): Promise<CourseStatsResponse> => {
  const response = await apiClient.get<CourseStatsResponse>(`${BASE_URL}/stats`)
  return response as any
}

export const getCourseEnrollmentStats = async (courseId: string): Promise<{
  totalEnrollments: number
  activeEnrollments: number
  completedEnrollments: number
  averageProgress: number
  completionRate: number
}> => {
  return apiClient.get(`${BASE_URL}/${courseId}/enrollment-stats`)
}
