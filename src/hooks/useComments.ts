import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

export interface Comment {
  id: string
  taskId: string
  userId: string
  content: string
  parentId?: string
  edited: boolean
  deleted: boolean
  authorName?: string
  authorEmail?: string
  createdAt: string
  updatedAt: string
  organizationId: string
}

interface CommentRequest {
  content: string
  parentId?: string
}

/**
 * Hook for managing task comments
 * Provides CRUD operations with React Query caching
 */
export function useComments(taskId: string) {
  const queryClient = useQueryClient()

  // Fetch all comments for a task
  const { data, isLoading, error, refetch } = useQuery<{ data: Comment[] }>({
    queryKey: ['comments', taskId],
    queryFn: async () => {
      const response = await fetch(`/api/tasks/${taskId}/comments`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error('Failed to fetch comments')
      }
      return response.json()
    },
    enabled: !!taskId,
  })

  // Add new comment
  const addMutation = useMutation({
    mutationFn: async ({ content, parentId }: CommentRequest) => {
      const response = await fetch(`/api/tasks/${taskId}/comments`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ content, parentId }),
      })
      if (!response.ok) {
        throw new Error('Failed to add comment')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
      toast.success('Comment added')
    },
    onError: () => {
      toast.error('Failed to add comment')
    },
  })

  // Update comment
  const updateMutation = useMutation({
    mutationFn: async ({ commentId, content }: { commentId: string; content: string }) => {
      const response = await fetch(`/api/tasks/comments/${commentId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ content }),
      })
      if (!response.ok) {
        throw new Error('Failed to update comment')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
      toast.success('Comment updated')
    },
    onError: () => {
      toast.error('Failed to update comment')
    },
  })

  // Delete comment
  const deleteMutation = useMutation({
    mutationFn: async (commentId: string) => {
      const response = await fetch(`/api/tasks/comments/${commentId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error('Failed to delete comment')
      }
      return response.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
      toast.success('Comment deleted')
    },
    onError: () => {
      toast.error('Failed to delete comment')
    },
  })

  // Organize comments into threads (top-level + replies)
  const organizeComments = (comments: Comment[]) => {
    const topLevel = comments.filter(c => !c.parentId && !c.deleted)
    const repliesMap = new Map<string, Comment[]>()

    comments.filter(c => c.parentId && !c.deleted).forEach(reply => {
      const parentId = reply.parentId!
      if (!repliesMap.has(parentId)) {
        repliesMap.set(parentId, [])
      }
      repliesMap.get(parentId)!.push(reply)
    })

    return { topLevel, repliesMap }
  }

  const comments = data?.data || []
  const { topLevel, repliesMap } = organizeComments(comments)

  // Helper function for adding replies
  const replyToComment = ({ parentId, content }: { parentId: string; content: string }) => {
    addMutation.mutate({ content, parentId })
  }

  return {
    comments,
    topLevelComments: topLevel,
    repliesMap,
    isLoading,
    error,
    refetch,
    addComment: addMutation.mutate,
    isAdding: addMutation.isPending,
    updateComment: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
    deleteComment: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending,
    replyToComment,
  }
}
