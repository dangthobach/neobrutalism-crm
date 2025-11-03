import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type {
  CourseEnrollment,
  CreateEnrollmentRequest,
  UpdateEnrollmentRequest,
  CourseProgressResponse,
  Certificate,
  EnrollmentFilters,
} from '@/types/course'
import * as enrollmentApi from '@/lib/api/course-enrollments-api'
import { courseKeys } from './useCourses'

// Query keys
export const enrollmentKeys = {
  all: ['course-enrollments'] as const,
  lists: () => [...enrollmentKeys.all, 'list'] as const,
  list: (filters?: EnrollmentFilters) => [...enrollmentKeys.lists(), { filters }] as const,
  details: () => [...enrollmentKeys.all, 'detail'] as const,
  detail: (id: string) => [...enrollmentKeys.details(), id] as const,
  byUser: (userId: string) => [...enrollmentKeys.all, 'user', userId] as const,
  byCourse: (courseId: string) => [...enrollmentKeys.all, 'course', courseId] as const,
  userCourse: (userId: string, courseId: string) => 
    [...enrollmentKeys.all, 'user', userId, 'course', courseId] as const,
  progress: (enrollmentId: string) => [...enrollmentKeys.detail(enrollmentId), 'progress'] as const,
  certificate: (enrollmentId: string) => [...enrollmentKeys.detail(enrollmentId), 'certificate'] as const,
}

// =====================================================
// Query Hooks
// =====================================================

export const useEnrollmentById = (id: string, enabled = true) => {
  return useQuery({
    queryKey: enrollmentKeys.detail(id),
    queryFn: () => enrollmentApi.getEnrollmentById(id),
    enabled: enabled && !!id,
  })
}

export const useAllEnrollments = (
  page: number = 0,
  size: number = 20,
  filters?: EnrollmentFilters
) => {
  return useQuery({
    queryKey: enrollmentKeys.list(filters),
    queryFn: () => enrollmentApi.getAllEnrollments(page, size, filters),
  })
}

export const useEnrollmentsByUser = (userId: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...enrollmentKeys.byUser(userId), { page, size }],
    queryFn: () => enrollmentApi.getEnrollmentsByUser(userId, page, size),
    enabled: !!userId,
  })
}

export const useEnrollmentsByCourse = (courseId: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...enrollmentKeys.byCourse(courseId), { page, size }],
    queryFn: () => enrollmentApi.getEnrollmentsByCourse(courseId, page, size),
    enabled: !!courseId,
  })
}

export const useUserEnrollment = (userId: string, courseId: string) => {
  return useQuery({
    queryKey: enrollmentKeys.userCourse(userId, courseId),
    queryFn: () => enrollmentApi.getUserEnrollment(userId, courseId),
    enabled: !!userId && !!courseId,
  })
}

export const useEnrollmentProgress = (enrollmentId: string, enabled = true) => {
  return useQuery({
    queryKey: enrollmentKeys.progress(enrollmentId),
    queryFn: () => enrollmentApi.getEnrollmentProgress(enrollmentId),
    enabled: enabled && !!enrollmentId,
  })
}

export const useCertificate = (enrollmentId: string, enabled = true) => {
  return useQuery({
    queryKey: enrollmentKeys.certificate(enrollmentId),
    queryFn: () => enrollmentApi.getCertificate(enrollmentId),
    enabled: enabled && !!enrollmentId,
  })
}

export const useVerifyCertificate = (verificationCode: string, enabled = true) => {
  return useQuery({
    queryKey: [...enrollmentKeys.all, 'verify', verificationCode],
    queryFn: () => enrollmentApi.verifyCertificate(verificationCode),
    enabled: enabled && !!verificationCode,
  })
}

// =====================================================
// Mutation Hooks
// =====================================================

export const useCreateEnrollment = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateEnrollmentRequest) => enrollmentApi.createEnrollment(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.byUser(variables.userId) })
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.byCourse(variables.courseId) })
      queryClient.invalidateQueries({ 
        queryKey: enrollmentKeys.userCourse(variables.userId, variables.courseId) 
      })
      queryClient.invalidateQueries({ queryKey: courseKeys.enrollmentStats(variables.courseId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) })
    },
  })
}

export const useUpdateEnrollment = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateEnrollmentRequest }) =>
      enrollmentApi.updateEnrollment(id, data),
    onSuccess: (enrollment) => {
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.detail(enrollment.id) })
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.byUser(enrollment.userId) })
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.byCourse(enrollment.courseId) })
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.progress(enrollment.id) })
    },
  })
}

export const useDeleteEnrollment = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, userId, courseId }: { id: string; userId: string; courseId: string }) =>
      enrollmentApi.deleteEnrollment(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.byUser(variables.userId) })
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.byCourse(variables.courseId) })
      queryClient.invalidateQueries({ 
        queryKey: enrollmentKeys.userCourse(variables.userId, variables.courseId) 
      })
      queryClient.invalidateQueries({ queryKey: courseKeys.enrollmentStats(variables.courseId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) })
    },
  })
}

export const useIssueCertificate = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (enrollmentId: string) => enrollmentApi.issueCertificate(enrollmentId),
    onSuccess: (_, enrollmentId) => {
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.detail(enrollmentId) })
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.certificate(enrollmentId) })
    },
  })
}
