import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

export interface ChecklistItem {
  id: string
  taskId: string
  title: string
  completed: boolean
  position: number
  organizationId: string
  createdAt: string
  updatedAt: string
}

export interface ChecklistProgress {
  total: number
  completed: number
  remaining: number
  percentage: number
}

interface ChecklistItemRequest {
  title: string
  completed?: boolean
}

interface ReorderRequest {
  itemIds: string[]
}

/**
 * Hook for managing task checklist items
 * Provides CRUD operations with React Query caching
 */
export function useChecklist(taskId: string) {
  const queryClient = useQueryClient()

  // Fetch all checklist items for a task
  const { data, isLoading, error, refetch } = useQuery<{ data: ChecklistItem[] }>({
    queryKey: ['checklist', taskId],
    queryFn: async () => {
      const response = await fetch(`/api/tasks/${taskId}/checklist`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error('Failed to fetch checklist items')
      }
      return response.json()
    },
    enabled: !!taskId,
  })

  // Add new checklist item
  const addMutation = useMutation({
    mutationFn: async (title: string) => {
      const response = await fetch(`/api/tasks/${taskId}/checklist`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title }),
      })
      if (!response.ok) {
        throw new Error('Failed to add checklist item')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checklist', taskId] })
      queryClient.invalidateQueries({ queryKey: ['checklist-progress', taskId] })
      toast.success('Checklist item added')
    },
    onError: () => {
      toast.error('Failed to add checklist item')
    },
  })

  // Toggle item completion
  const toggleMutation = useMutation({
    mutationFn: async (itemId: string) => {
      const response = await fetch(`/api/tasks/checklist/${itemId}/toggle`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error('Failed to toggle checklist item')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checklist', taskId] })
      queryClient.invalidateQueries({ queryKey: ['checklist-progress', taskId] })
    },
    onError: () => {
      toast.error('Failed to toggle checklist item')
    },
  })

  // Update item (title or completed status)
  const updateMutation = useMutation({
    mutationFn: async ({ itemId, title, completed }: { itemId: string; title?: string; completed?: boolean }) => {
      const response = await fetch(`/api/tasks/checklist/${itemId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title, completed }),
      })
      if (!response.ok) {
        throw new Error('Failed to update checklist item')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checklist', taskId] })
      queryClient.invalidateQueries({ queryKey: ['checklist-progress', taskId] })
      toast.success('Checklist item updated')
    },
    onError: () => {
      toast.error('Failed to update checklist item')
    },
  })

  // Delete item (soft delete)
  const deleteMutation = useMutation({
    mutationFn: async (itemId: string) => {
      const response = await fetch(`/api/tasks/checklist/${itemId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error('Failed to delete checklist item')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checklist', taskId] })
      queryClient.invalidateQueries({ queryKey: ['checklist-progress', taskId] })
      toast.success('Checklist item deleted')
    },
    onError: () => {
      toast.error('Failed to delete checklist item')
    },
  })

  // Reorder items (drag & drop)
  const reorderMutation = useMutation({
    mutationFn: async (itemIds: string[]) => {
      const response = await fetch(`/api/tasks/${taskId}/checklist/reorder`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ itemIds }),
      })
      if (!response.ok) {
        throw new Error('Failed to reorder checklist items')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checklist', taskId] })
    },
    onError: () => {
      toast.error('Failed to reorder checklist items')
    },
  })

  return {
    items: data?.data || [],
    isLoading,
    error,
    refetch,
    addItem: addMutation.mutate,
    isAdding: addMutation.isPending,
    toggleItem: toggleMutation.mutate,
    isToggling: toggleMutation.isPending,
    updateItem: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
    deleteItem: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending,
    reorderItems: reorderMutation.mutate,
    isReordering: reorderMutation.isPending,
  }
}

/**
 * Hook for fetching checklist progress
 */
export function useChecklistProgress(taskId: string) {
  return useQuery<{ data: ChecklistProgress }>({
    queryKey: ['checklist-progress', taskId],
    queryFn: async () => {
      const response = await fetch(`/api/tasks/${taskId}/checklist/progress`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error('Failed to fetch checklist progress')
      }
      return response.json()
    },
    enabled: !!taskId,
  })
}
