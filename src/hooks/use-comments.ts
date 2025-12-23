/**
 * Task Comments React Query Hooks
 * Provides data fetching and mutations for task comments with WebSocket support
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { useWebSocket } from '@/hooks/use-websocket'

// Types
export interface Comment {
  id: string
  taskId: string
  userId: string
  content: string
  parentId?: string
  edited: boolean
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateCommentRequest {
  content: string
  parentId?: string
}

export interface UpdateCommentRequest {
  content: string
}

// API functions
const commentApi = {
  getAll: async (taskId: string): Promise<Comment[]> => {
    const response = await fetch(`/api/tasks/${taskId}/comments`)
    if (!response.ok) throw new Error('Failed to fetch comments')
    const data = await response.json()
    return data.data
  },

  getPaginated: async (taskId: string, page = 0, size = 20) => {
    const response = await fetch(`/api/tasks/${taskId}/comments/paginated?page=${page}&size=${size}`)
    if (!response.ok) throw new Error('Failed to fetch comments')
    const data = await response.json()
    return data.data
  },

  getCount: async (taskId: string): Promise<number> => {
    const response = await fetch(`/api/tasks/${taskId}/comments/count`)
    if (!response.ok) throw new Error('Failed to fetch comment count')
    const data = await response.json()
    return data.data
  },

  create: async (taskId: string, request: CreateCommentRequest): Promise<Comment> => {
    const response = await fetch(`/api/tasks/${taskId}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    if (!response.ok) throw new Error('Failed to create comment')
    const data = await response.json()
    return data.data
  },

  update: async (commentId: string, request: UpdateCommentRequest): Promise<Comment> => {
    const response = await fetch(`/api/tasks/comments/${commentId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    if (!response.ok) throw new Error('Failed to update comment')
    const data = await response.json()
    return data.data
  },

  delete: async (commentId: string): Promise<void> => {
    const response = await fetch(`/api/tasks/comments/${commentId}`, {
      method: 'DELETE',
    })
    if (!response.ok) throw new Error('Failed to delete comment')
  },
}

// Query keys
export const commentKeys = {
  all: ['comments'] as const,
  byTask: (taskId: string) => [...commentKeys.all, 'task', taskId] as const,
  count: (taskId: string) => [...commentKeys.all, 'count', taskId] as const,
}

/**
 * Get all comments for a task
 */
export function useComments(taskId: string | undefined) {
  return useQuery({
    queryKey: commentKeys.byTask(taskId!),
    queryFn: () => commentApi.getAll(taskId!),
    enabled: !!taskId,
  })
}

/**
 * Get comment count for a task
 */
export function useCommentCount(taskId: string | undefined) {
  return useQuery({
    queryKey: commentKeys.count(taskId!),
    queryFn: () => commentApi.getCount(taskId!),
    enabled: !!taskId,
  })
}

/**
 * Create comment mutation
 */
export function useCreateComment(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateCommentRequest) => commentApi.create(taskId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: commentKeys.byTask(taskId) })
      queryClient.invalidateQueries({ queryKey: commentKeys.count(taskId) })
      toast.success('Comment added')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to add comment')
    },
  })
}

/**
 * Update comment mutation
 */
export function useUpdateComment(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ commentId, request }: { commentId: string; request: UpdateCommentRequest }) =>
      commentApi.update(commentId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: commentKeys.byTask(taskId) })
      toast.success('Comment updated')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to update comment')
    },
  })
}

/**
 * Delete comment mutation
 */
export function useDeleteComment(taskId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (commentId: string) => commentApi.delete(commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: commentKeys.byTask(taskId) })
      queryClient.invalidateQueries({ queryKey: commentKeys.count(taskId) })
      toast.success('Comment deleted')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to delete comment')
    },
  })
}

/**
 * Hook for WebSocket real-time comment updates
 */
export function useCommentRealtime(taskId: string | undefined) {
  const queryClient = useQueryClient()
  const { subscribe, unsubscribe } = useWebSocket()

  useEffect(() => {
    if (!taskId) return

    const topic = `/topic/tasks/${taskId}/comments`
    
    const handleCommentUpdate = (message: any) => {
      const { action } = message
      
      // Invalidate queries to refetch comments
      if (action === 'COMMENT_ADDED' || action === 'COMMENT_UPDATED' || action === 'COMMENT_DELETED') {
        queryClient.invalidateQueries({ queryKey: commentKeys.byTask(taskId) })
        queryClient.invalidateQueries({ queryKey: commentKeys.count(taskId) })
      }
    }

    subscribe(topic, handleCommentUpdate)

    return () => {
      unsubscribe(topic)
    }
  }, [taskId, subscribe, unsubscribe, queryClient])
}
