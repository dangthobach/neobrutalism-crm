import { apiClient } from './client'
import type {
  CourseEnrollment,
  CreateEnrollmentRequest,
  UpdateEnrollmentRequest,
  CourseProgressResponse,
  Certificate,
  PageResponse,
  EnrollmentFilters,
} from '@/types/course'

const BASE_URL = '/api/course-enrollments'

export const createEnrollment = async (data: CreateEnrollmentRequest): Promise<CourseEnrollment> => {
  const response = await apiClient.post<CourseEnrollment>(BASE_URL, data)
  return response as any
}

export const updateEnrollment = async (
  id: string,
  data: UpdateEnrollmentRequest
): Promise<CourseEnrollment> => {
  const response = await apiClient.put<CourseEnrollment>(`${BASE_URL}/${id}`, data)
  return response as any
}

export const deleteEnrollment = async (id: string): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/${id}`)
}

export const getEnrollmentById = async (id: string): Promise<CourseEnrollment> => {
  const response = await apiClient.get<CourseEnrollment>(`${BASE_URL}/${id}`)
  return response as any
}

export const getAllEnrollments = async (
  page: number = 0,
  size: number = 20,
  filters?: EnrollmentFilters
): Promise<PageResponse<CourseEnrollment>> => {
  const params = new URLSearchParams()
  params.append('page', page.toString())
  params.append('size', size.toString())

  if (filters) {
    if (filters.status) params.append('status', filters.status)
    if (filters.courseId) params.append('courseId', filters.courseId)
    if (filters.userId) params.append('userId', filters.userId)
    if (filters.minProgress !== undefined)
      params.append('minProgress', filters.minProgress.toString())
    if (filters.maxProgress !== undefined)
      params.append('maxProgress', filters.maxProgress.toString())
  }

  const response = await apiClient.get<PageResponse<CourseEnrollment>>(`${BASE_URL}?${params}`)
  return response as any
}

export const getEnrollmentsByUser = async (
  userId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<CourseEnrollment>> => {
  const response = await apiClient.get<PageResponse<CourseEnrollment>>(
    `${BASE_URL}/user/${userId}?page=${page}&size=${size}`
  )
  return response as any
}

export const getEnrollmentsByCourse = async (
  courseId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<CourseEnrollment>> => {
  const response = await apiClient.get<PageResponse<CourseEnrollment>>(
    `${BASE_URL}/course/${courseId}?page=${page}&size=${size}`
  )
  return response as any
}

export const getUserEnrollment = async (userId: string, courseId: string): Promise<CourseEnrollment | null> => {
  try {
    const response = await apiClient.get<CourseEnrollment>(
      `${BASE_URL}/user/${userId}/course/${courseId}`
    )
    return response as any
  } catch (error) {
    return null
  }
}

export const getEnrollmentProgress = async (enrollmentId: string): Promise<CourseProgressResponse> => {
  const response = await apiClient.get<CourseProgressResponse>(`${BASE_URL}/${enrollmentId}/progress`)
  return response as any
}

export const issueCertificate = async (enrollmentId: string): Promise<Certificate> => {
  const response = await apiClient.post<Certificate>(`${BASE_URL}/${enrollmentId}/certificate`)
  return response as any
}

export const getCertificate = async (enrollmentId: string): Promise<Certificate> => {
  const response = await apiClient.get<Certificate>(`${BASE_URL}/${enrollmentId}/certificate`)
  return response as any
}

export const verifyCertificate = async (verificationCode: string): Promise<Certificate> => {
  const response = await apiClient.get<Certificate>(`${BASE_URL}/certificates/verify/${verificationCode}`)
  return response as any
}
