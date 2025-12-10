import { apiClient } from './client'
import type {
  CourseModule,
  CreateModuleRequest,
  UpdateModuleRequest,
} from '@/types/course'

const BASE_URL = '/api/course-modules'

export const createModule = async (data: CreateModuleRequest): Promise<CourseModule> => {
  const response = await apiClient.post<CourseModule>(BASE_URL, data)
  return response as any
}

export const updateModule = async (id: string, data: UpdateModuleRequest): Promise<CourseModule> => {
  const response = await apiClient.put<CourseModule>(`${BASE_URL}/${id}`, data)
  return response as any
}

export const deleteModule = async (id: string): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/${id}`)
}

export const getModuleById = async (id: string): Promise<CourseModule> => {
  const response = await apiClient.get<CourseModule>(`${BASE_URL}/${id}`)
  return response as any
}

export const getModulesByCourse = async (courseId: string): Promise<CourseModule[]> => {
  const response = await apiClient.get<CourseModule[]>(`${BASE_URL}/course/${courseId}`)
  return response as any
}

export const reorderModules = async (courseId: string, moduleIds: string[]): Promise<CourseModule[]> => {
  const response = await apiClient.post<CourseModule[]>(`${BASE_URL}/course/${courseId}/reorder`, {
    moduleIds,
  })
  return response as any
}
