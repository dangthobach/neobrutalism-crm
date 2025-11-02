import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type {
  Course,
  CreateCourseRequest,
  UpdateCourseRequest,
  CourseStatsResponse,
  CourseFilters,
} from '@/types/course'
import * as courseApi from '@/lib/api/course-api'

// Query keys
export const courseKeys = {
  all: ['courses'] as const,
  lists: () => [...courseKeys.all, 'list'] as const,
  list: (filters?: CourseFilters) => [...courseKeys.lists(), { filters }] as const,
  published: () => [...courseKeys.all, 'published'] as const,
  details: () => [...courseKeys.all, 'detail'] as const,
  detail: (id: string) => [...courseKeys.details(), id] as const,
  bySlug: (slug: string) => [...courseKeys.all, 'slug', slug] as const,
  byInstructor: (instructorId: string) => [...courseKeys.all, 'instructor', instructorId] as const,
  byCategory: (categoryId: string) => [...courseKeys.all, 'category', categoryId] as const,
  featured: () => [...courseKeys.all, 'featured'] as const,
  popular: () => [...courseKeys.all, 'popular'] as const,
  stats: () => [...courseKeys.all, 'stats'] as const,
  enrollmentStats: (courseId: string) => [...courseKeys.detail(courseId), 'enrollment-stats'] as const,
}

// =====================================================
// Query Hooks
// =====================================================

export const useAllCourses = (page: number = 0, size: number = 20, filters?: CourseFilters) => {
  return useQuery({
    queryKey: courseKeys.list(filters),
    queryFn: () => courseApi.getAllCourses(page, size, filters),
  })
}

export const usePublishedCourses = (page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...courseKeys.published(), { page, size }],
    queryFn: () => courseApi.getPublishedCourses(page, size),
  })
}

export const useCourseById = (id: string, enabled = true) => {
  return useQuery({
    queryKey: courseKeys.detail(id),
    queryFn: () => courseApi.getCourseById(id),
    enabled: enabled && !!id,
  })
}

export const useCourseBySlug = (slug: string, enabled = true) => {
  return useQuery({
    queryKey: courseKeys.bySlug(slug),
    queryFn: () => courseApi.getCourseBySlug(slug),
    enabled: enabled && !!slug,
  })
}

export const useCoursesByInstructor = (instructorId: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...courseKeys.byInstructor(instructorId), { page, size }],
    queryFn: () => courseApi.getCoursesByInstructor(instructorId, page, size),
    enabled: !!instructorId,
  })
}

export const useCoursesByCategory = (categoryId: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...courseKeys.byCategory(categoryId), { page, size }],
    queryFn: () => courseApi.getCoursesByCategory(categoryId, page, size),
    enabled: !!categoryId,
  })
}

export const useFeaturedCourses = (limit: number = 6) => {
  return useQuery({
    queryKey: [...courseKeys.featured(), { limit }],
    queryFn: () => courseApi.getFeaturedCourses(limit),
  })
}

export const usePopularCourses = (limit: number = 6) => {
  return useQuery({
    queryKey: [...courseKeys.popular(), { limit }],
    queryFn: () => courseApi.getPopularCourses(limit),
  })
}

export const useSearchCourses = (keyword: string, page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...courseKeys.all, 'search', { keyword, page, size }],
    queryFn: () => courseApi.searchCourses(keyword, page, size),
    enabled: keyword.length > 0,
  })
}

export const useCourseStats = () => {
  return useQuery({
    queryKey: courseKeys.stats(),
    queryFn: () => courseApi.getCourseStats(),
  })
}

export const useCourseEnrollmentStats = (courseId: string) => {
  return useQuery({
    queryKey: courseKeys.enrollmentStats(courseId),
    queryFn: () => courseApi.getCourseEnrollmentStats(courseId),
    enabled: !!courseId,
  })
}

// =====================================================
// Mutation Hooks
// =====================================================

export const useCreateCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateCourseRequest) => courseApi.createCourse(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: courseKeys.stats() })
    },
  })
}

export const useUpdateCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCourseRequest }) =>
      courseApi.updateCourse(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
    },
  })
}

export const useDeleteCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => courseApi.deleteCourse(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: courseKeys.stats() })
    },
  })
}

export const usePublishCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => courseApi.publishCourse(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(id) })
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: courseKeys.published() })
      queryClient.invalidateQueries({ queryKey: courseKeys.stats() })
    },
  })
}

export const useUnpublishCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => courseApi.unpublishCourse(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(id) })
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: courseKeys.published() })
      queryClient.invalidateQueries({ queryKey: courseKeys.stats() })
    },
  })
}

export const useArchiveCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => courseApi.archiveCourse(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(id) })
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: courseKeys.stats() })
    },
  })
}

export const useDuplicateCourse = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => courseApi.duplicateCourse(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: courseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: courseKeys.stats() })
    },
  })
}
