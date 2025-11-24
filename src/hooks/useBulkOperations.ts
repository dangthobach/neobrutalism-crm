import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

interface BulkOperationResponse {
  totalRequested: number
  successCount: number
  failureCount: number
  successfulTaskIds: string[]
  errors: Array<{
    taskId: string
    message: string
  }>
}

interface BulkAssignRequest {
  taskIds: string[]
  assigneeId: string
}

interface BulkStatusChangeRequest {
  taskIds: string[]
  status: string
}

/**
 * Hook for bulk task operations
 * Provides mutations for bulk assign, status change, and delete
 * with React Query caching and optimistic updates
 */
export function useBulkOperations() {
  const queryClient = useQueryClient()

  // Bulk assign tasks to a user
  const bulkAssignMutation = useMutation({
    mutationFn: async ({ taskIds, assigneeId }: BulkAssignRequest) => {
      const response = await fetch('/api/tasks/bulk/assign', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ taskIds, assigneeId }),
      })
      if (!response.ok) {
        throw new Error('Failed to bulk assign tasks')
      }
      return response.json() as Promise<{ data: BulkOperationResponse }>
    },
    onSuccess: (data) => {
      const result = data.data
      queryClient.invalidateQueries({ queryKey: ['tasks'] })

      if (result.failureCount > 0) {
        toast.warning(
          `Assigned ${result.successCount} tasks. ${result.failureCount} failed.`,
          { description: 'Some tasks could not be assigned' }
        )
      } else {
        toast.success(`Successfully assigned ${result.successCount} tasks`)
      }
    },
    onError: () => {
      toast.error('Failed to assign tasks')
    },
  })

  // Bulk change task status
  const bulkStatusChangeMutation = useMutation({
    mutationFn: async ({ taskIds, status }: BulkStatusChangeRequest) => {
      const response = await fetch('/api/tasks/bulk/status', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ taskIds, status }),
      })
      if (!response.ok) {
        throw new Error('Failed to bulk change status')
      }
      return response.json() as Promise<{ data: BulkOperationResponse }>
    },
    onSuccess: (data) => {
      const result = data.data
      queryClient.invalidateQueries({ queryKey: ['tasks'] })

      if (result.failureCount > 0) {
        toast.warning(
          `Updated ${result.successCount} tasks. ${result.failureCount} failed.`,
          { description: 'Some tasks could not be updated' }
        )
      } else {
        toast.success(`Successfully updated ${result.successCount} tasks`)
      }
    },
    onError: () => {
      toast.error('Failed to update task status')
    },
  })

  // Bulk delete tasks (soft delete)
  const bulkDeleteMutation = useMutation({
    mutationFn: async (taskIds: string[]) => {
      const response = await fetch('/api/tasks/bulk', {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(taskIds),
      })
      if (!response.ok) {
        throw new Error('Failed to bulk delete tasks')
      }
      return response.json() as Promise<{ data: BulkOperationResponse }>
    },
    onSuccess: (data) => {
      const result = data.data
      queryClient.invalidateQueries({ queryKey: ['tasks'] })

      if (result.failureCount > 0) {
        toast.warning(
          `Deleted ${result.successCount} tasks. ${result.failureCount} failed.`,
          { description: 'Some tasks could not be deleted' }
        )
      } else {
        toast.success(`Successfully deleted ${result.successCount} tasks`)
      }
    },
    onError: () => {
      toast.error('Failed to delete tasks')
    },
  })

  return {
    bulkAssign: bulkAssignMutation.mutate,
    isBulkAssigning: bulkAssignMutation.isPending,
    bulkStatusChange: bulkStatusChangeMutation.mutate,
    isBulkChangingStatus: bulkStatusChangeMutation.isPending,
    bulkDelete: bulkDeleteMutation.mutate,
    isBulkDeleting: bulkDeleteMutation.isPending,
  }
}
