import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type {
  CourseLesson,
  CreateLessonRequest,
  UpdateLessonRequest,
} from '@/types/course'
import * as lessonApi from '@/lib/api/course-lessons-api'
import { moduleKeys } from './useCourseModules'
import { courseKeys } from './useCourses'

// Query keys
export const lessonKeys = {
  all: ['course-lessons'] as const,
  details: () => [...lessonKeys.all, 'detail'] as const,
  detail: (id: string) => [...lessonKeys.details(), id] as const,
  byModule: (moduleId: string) => [...lessonKeys.all, 'module', moduleId] as const,
  byCourse: (courseId: string) => [...lessonKeys.all, 'course', courseId] as const,
}

// =====================================================
// Query Hooks
// =====================================================

export const useLessonById = (id: string, enabled = true) => {
  return useQuery({
    queryKey: lessonKeys.detail(id),
    queryFn: () => lessonApi.getLessonById(id),
    enabled: enabled && !!id,
  })
}

export const useLessonsByModule = (moduleId: string, enabled = true) => {
  return useQuery({
    queryKey: lessonKeys.byModule(moduleId),
    queryFn: () => lessonApi.getLessonsByModule(moduleId),
    enabled: enabled && !!moduleId,
  })
}

export const useLessonsByCourse = (courseId: string, enabled = true) => {
  return useQuery({
    queryKey: lessonKeys.byCourse(courseId),
    queryFn: () => lessonApi.getLessonsByCourse(courseId),
    enabled: enabled && !!courseId,
  })
}

// =====================================================
// Mutation Hooks
// =====================================================

export const useCreateLesson = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateLessonRequest) => lessonApi.createLesson(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: lessonKeys.byModule(variables.moduleId) })
      queryClient.invalidateQueries({ queryKey: lessonKeys.byCourse(variables.courseId) })
      queryClient.invalidateQueries({ queryKey: moduleKeys.detail(variables.moduleId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) })
    },
  })
}

export const useUpdateLesson = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLessonRequest }) =>
      lessonApi.updateLesson(id, data),
    onSuccess: (lesson) => {
      queryClient.invalidateQueries({ queryKey: lessonKeys.detail(lesson.id) })
      queryClient.invalidateQueries({ queryKey: lessonKeys.byModule(lesson.moduleId) })
      queryClient.invalidateQueries({ queryKey: lessonKeys.byCourse(lesson.courseid) })
      queryClient.invalidateQueries({ queryKey: moduleKeys.detail(lesson.moduleId) })
    },
  })
}

export const useDeleteLesson = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, moduleId, courseId }: { id: string; moduleId: string; courseId: string }) =>
      lessonApi.deleteLesson(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: lessonKeys.byModule(variables.moduleId) })
      queryClient.invalidateQueries({ queryKey: lessonKeys.byCourse(variables.courseId) })
      queryClient.invalidateQueries({ queryKey: moduleKeys.detail(variables.moduleId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) })
    },
  })
}

export const useReorderLessons = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ moduleId, lessonIds }: { moduleId: string; lessonIds: string[] }) =>
      lessonApi.reorderLessons(moduleId, lessonIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: lessonKeys.byModule(variables.moduleId) })
    },
  })
}

export const useMarkLessonComplete = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ enrollmentId, lessonId }: { enrollmentId: string; lessonId: string }) =>
      lessonApi.markLessonComplete(enrollmentId, lessonId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', 'detail', variables.enrollmentId] })
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', variables.enrollmentId, 'progress'] })
    },
  })
}

export const useTrackLessonProgress = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      enrollmentId,
      lessonId,
      progressPercentage,
      timeSpent,
    }: {
      enrollmentId: string
      lessonId: string
      progressPercentage: number
      timeSpent: number
    }) => lessonApi.trackLessonProgress(enrollmentId, lessonId, progressPercentage, timeSpent),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', 'detail', variables.enrollmentId] })
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', variables.enrollmentId, 'progress'] })
    },
  })
}
