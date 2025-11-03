import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type {
  CourseModule,
  CreateModuleRequest,
  UpdateModuleRequest,
} from '@/types/course'
import * as moduleApi from '@/lib/api/course-modules-api'
import { courseKeys } from './useCourses'

// Query keys
export const moduleKeys = {
  all: ['course-modules'] as const,
  details: () => [...moduleKeys.all, 'detail'] as const,
  detail: (id: string) => [...moduleKeys.details(), id] as const,
  byCourse: (courseId: string) => [...moduleKeys.all, 'course', courseId] as const,
}

// =====================================================
// Query Hooks
// =====================================================

export const useModuleById = (id: string, enabled = true) => {
  return useQuery({
    queryKey: moduleKeys.detail(id),
    queryFn: () => moduleApi.getModuleById(id),
    enabled: enabled && !!id,
  })
}

export const useModulesByCourse = (courseId: string, enabled = true) => {
  return useQuery({
    queryKey: moduleKeys.byCourse(courseId),
    queryFn: () => moduleApi.getModulesByCourse(courseId),
    enabled: enabled && !!courseId,
  })
}

// =====================================================
// Mutation Hooks
// =====================================================

export const useCreateModule = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateModuleRequest) => moduleApi.createModule(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: moduleKeys.byCourse(variables.courseId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) })
    },
  })
}

export const useUpdateModule = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateModuleRequest }) =>
      moduleApi.updateModule(id, data),
    onSuccess: (module) => {
      queryClient.invalidateQueries({ queryKey: moduleKeys.detail(module.id) })
      queryClient.invalidateQueries({ queryKey: moduleKeys.byCourse(module.courseId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(module.courseId) })
    },
  })
}

export const useDeleteModule = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, courseId }: { id: string; courseId: string }) => moduleApi.deleteModule(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: moduleKeys.byCourse(variables.courseId) })
      queryClient.invalidateQueries({ queryKey: courseKeys.detail(variables.courseId) })
    },
  })
}

export const useReorderModules = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ courseId, moduleIds }: { courseId: string; moduleIds: string[] }) =>
      moduleApi.reorderModules(courseId, moduleIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: moduleKeys.byCourse(variables.courseId) })
    },
  })
}
