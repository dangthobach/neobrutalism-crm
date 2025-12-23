/**
 * Task Checklist React Query Hooks
 * Provides data fetching and mutations for task checklist items
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

// Types
export interface ChecklistItem {
  id: string
  taskId: string
  title: string
  completed: boolean
  position: number
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateChecklistItemRequest {
  title: string
}

export interface UpdateChecklistItemRequest {
  title?: string
  completed?: boolean
}

export interface ChecklistProgress {
  total: number
  completed: number
  remaining: number
  percentage: number
}

export interface ReorderChecklistRequest {
  itemIds: string[]
}

// API functions
const checklistApi = {
  getAll: async (taskId: string): Promise<ChecklistItem[]> => {
    const response = await fetch(`/api/tasks/${taskId}/checklist`)
    if (!response.ok) throw new Error('Failed to fetch checklist')
    const data = await response.json()
    return data.data
  },

  getProgress: async (taskId: string): Promise<ChecklistProgress> => {
    const response = await fetch(`/api/tasks/${taskId}/checklist/progress`)
    if (!response.ok) throw new Error('Failed to fetch progress')
    const data = await response.json()
    return data.data
  },

  create: async (taskId: string, request: CreateChecklistItemRequest): Promise<ChecklistItem> => {
    const response = await fetch(`/api/tasks/${taskId}/checklist`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    if (!response.ok) throw new Error('Failed to create checklist item')
    const data = await response.json()
    return data.data
  },

  update: async (itemId: string, request: UpdateChecklistItemRequest): Promise<ChecklistItem> => {
    const response = await fetch(`/api/tasks/checklist/${itemId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    if (!response.ok) throw new Error('Failed to update checklist item')
    const data = await response.json()
    return data.data
  },

  toggle: async (itemId: string): Promise<ChecklistItem> => {
    const response = await fetch(`/api/tasks/checklist/${itemId}/toggle`, {
      method: 'PUT',
    })
    if (!response.ok) throw new Error('Failed to toggle checklist item')
    const data = await response.json()
    return data.data
  },

  reorder: async (taskId: string, request: ReorderChecklistRequest): Promise<ChecklistItem[]> => {
    const response = await fetch(`/api/tasks/${taskId}/checklist/reorder`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    if (!response.ok) throw new Error('Failed to reorder checklist')
    const data = await response.json()
    return data.data
  },

  delete: async (itemId: string): Promise<void> => {
    const response = await fetch(`/api/tasks/checklist/${itemId}`, {
      method: 'DELETE',
    })
    if (!response.ok) throw new Error('Failed to delete checklist item')
  },
}

// Query keys
export const checklistKeys = {
  all: ['checklist'] as const,
  byTask: (taskId: string) => [...checklistKeys.all, 'task', taskId] as const,
  progress: (taskId: string) => [...checklistKeys.all, 'progress', taskId] as const,
}

/**
 * Get all checklist items for a task
 */
export function useChecklist(taskId: string | undefined) {
  return useQuery({
    queryKey: checklistKeys.byTask(taskId!),
    queryFn: () => checklistApi.getAll(taskId!),
    enabled: !!taskId,
  })
}

/**
 * Get checklist progress
 */
export function useChecklistProgress(taskId: string | undefined) {
  return useQuery({
    queryKey: checklistKeys.progress(taskId!),
    queryFn: () => checklistApi.getProgress(taskId!),
    enabled: !!taskId,
  })
}

/**
 * Create checklist item mutation
 */
export function useCreateChecklistItem(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateChecklistItemRequest) => checklistApi.create(taskId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.byTask(taskId) })
      queryClient.invalidateQueries({ queryKey: checklistKeys.progress(taskId) })
      toast.success('Checklist item added')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to add checklist item')
    },
  })
}

/**
 * Update checklist item mutation
 */
export function useUpdateChecklistItem(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ itemId, request }: { itemId: string; request: UpdateChecklistItemRequest }) =>
      checklistApi.update(itemId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.byTask(taskId) })
      queryClient.invalidateQueries({ queryKey: checklistKeys.progress(taskId) })
      toast.success('Checklist item updated')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to update checklist item')
    },
  })
}

/**
 * Toggle checklist item mutation
 */
export function useToggleChecklistItem(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (itemId: string) => checklistApi.toggle(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.byTask(taskId) })
      queryClient.invalidateQueries({ queryKey: checklistKeys.progress(taskId) })
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to toggle checklist item')
    },
  })
}

/**
 * Reorder checklist items mutation
 */
export function useReorderChecklist(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ReorderChecklistRequest) => checklistApi.reorder(taskId, request),
    onMutate: async (request) => {
      // Optimistic update
      await queryClient.cancelQueries({ queryKey: checklistKeys.byTask(taskId) })
      const previousItems = queryClient.getQueryData<ChecklistItem[]>(checklistKeys.byTask(taskId))

      if (previousItems) {
        const reordered = request.itemIds.map((id, index) => {
          const item = previousItems.find((i) => i.id === id)
          return item ? { ...item, position: index } : null
        }).filter(Boolean) as ChecklistItem[]

        queryClient.setQueryData(checklistKeys.byTask(taskId), reordered)
      }

      return { previousItems }
    },
    onError: (error: Error, _variables, context) => {
      // Rollback on error
      if (context?.previousItems) {
        queryClient.setQueryData(checklistKeys.byTask(taskId), context.previousItems)
      }
      toast.error(error.message || 'Failed to reorder checklist')
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.byTask(taskId) })
    },
  })
}

/**
 * Delete checklist item mutation
 */
export function useDeleteChecklistItem(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (itemId: string) => checklistApi.delete(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.byTask(taskId) })
      queryClient.invalidateQueries({ queryKey: checklistKeys.progress(taskId) })
      toast.success('Checklist item deleted')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to delete checklist item')
    },
  })
}
