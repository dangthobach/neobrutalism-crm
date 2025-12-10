import { apiClient } from './client'
import type {
  CourseLesson,
  CreateLessonRequest,
  UpdateLessonRequest,
} from '@/types/course'

const BASE_URL = '/api/course-lessons'

export const createLesson = async (data: CreateLessonRequest): Promise<CourseLesson> => {
  const response = await apiClient.post<CourseLesson>(BASE_URL, data)
  return response as any
}

export const updateLesson = async (id: string, data: UpdateLessonRequest): Promise<CourseLesson> => {
  const response = await apiClient.put<CourseLesson>(`${BASE_URL}/${id}`, data)
  return response as any
}

export const deleteLesson = async (id: string): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/${id}`)
}

export const getLessonById = async (id: string): Promise<CourseLesson> => {
  const response = await apiClient.get<CourseLesson>(`${BASE_URL}/${id}`)
  return response as any
}

export const getLessonsByModule = async (moduleId: string): Promise<CourseLesson[]> => {
  const response = await apiClient.get<CourseLesson[]>(`${BASE_URL}/module/${moduleId}`)
  return response as any
}

export const getLessonsByCourse = async (courseId: string): Promise<CourseLesson[]> => {
  const response = await apiClient.get<CourseLesson[]>(`${BASE_URL}/course/${courseId}`)
  return response as any
}

export const reorderLessons = async (moduleId: string, lessonIds: string[]): Promise<CourseLesson[]> => {
  const response = await apiClient.post<CourseLesson[]>(`${BASE_URL}/module/${moduleId}/reorder`, {
    lessonIds,
  })
  return response as any
}

export const markLessonComplete = async (enrollmentId: string, lessonId: string): Promise<void> => {
  await apiClient.post(`${BASE_URL}/${lessonId}/complete`, { enrollmentId })
}

export const trackLessonProgress = async (
  enrollmentId: string,
  lessonId: string,
  progressPercentage: number,
  timeSpent: number
): Promise<void> => {
  await apiClient.post(`${BASE_URL}/${lessonId}/track-progress`, {
    enrollmentId,
    progressPercentage,
    timeSpent,
  })
}
