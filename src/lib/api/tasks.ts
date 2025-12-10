/**
 * Task API Client
 * Handles all task-related API calls
 */

import { apiClient, PageResponse } from './client'
import type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskSearchParams,
  TaskStats,
  TaskBoard,
  TaskComment,
  CreateTaskCommentRequest,
} from '@/types/task'

export const taskApi = {
  /**
   * Get paginated list of tasks
   */
  getAll: async (params?: TaskSearchParams): Promise<PageResponse<Task>> => {
    const response = await apiClient.get<PageResponse<Task>>('/tasks', params)
    return response as any
  },

  /**
   * Get task by ID
   */
  getById: async (id: string): Promise<Task> => {
    const response = await apiClient.get<Task>(`/tasks/${id}`)
    return response as any
  },

  /**
   * Create new task
   */
  create: async (data: CreateTaskRequest): Promise<Task> => {
    const response = await apiClient.post<Task>('/tasks', data)
    return response as any
  },

  /**
   * Update task
   */
  update: async (id: string, data: UpdateTaskRequest): Promise<Task> => {
    const response = await apiClient.put<Task>(`/tasks/${id}`, data)
    return response as any
  },

  /**
   * Delete task
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/tasks/${id}`)
  },

  /**
   * Get tasks by customer
   */
  getByCustomer: async (customerId: string, params?: TaskSearchParams): Promise<PageResponse<Task>> => {
    const response = await apiClient.get<PageResponse<Task>>(`/tasks/customer/${customerId}`, params)
    return response as any
  },

  /**
   * Get tasks by contact
   */
  getByContact: async (contactId: string, params?: TaskSearchParams): Promise<PageResponse<Task>> => {
    const response = await apiClient.get<PageResponse<Task>>(`/tasks/contact/${contactId}`, params)
    return response as any
  },

  /**
   * Get tasks assigned to user
   */
  getByAssignee: async (userId: string, params?: TaskSearchParams): Promise<PageResponse<Task>> => {
    const response = await apiClient.get<PageResponse<Task>>(`/tasks/assigned/${userId}`, params)
    return response as any
  },

  /**
   * Get task board (Kanban view)
   */
  getBoard: async (params?: Omit<TaskSearchParams, 'status'>): Promise<TaskBoard> => {
    const response = await apiClient.get<TaskBoard>('/tasks/board', params)
    return response as any
  },

  /**
   * Get my tasks
   */
  getMyTasks: async (params?: TaskSearchParams): Promise<PageResponse<Task>> => {
    const response = await apiClient.get<PageResponse<Task>>('/tasks/my-tasks', params)
    return response as any
  },

  /**
   * Get overdue tasks
   */
  getOverdue: async (params?: TaskSearchParams): Promise<Task[]> => {
    const response = await apiClient.get<Task[]>('/tasks/overdue', params)
    return response as any
  },

  /**
   * Get tasks due today
   */
  getDueToday: async (params?: TaskSearchParams): Promise<Task[]> => {
    const response = await apiClient.get<Task[]>('/tasks/due-today', params)
    return response as any
  },

  /**
   * Get tasks due this week
   */
  getDueThisWeek: async (params?: TaskSearchParams): Promise<Task[]> => {
    const response = await apiClient.get<Task[]>('/tasks/due-this-week', params)
    return response as any
  },

  /**
   * Change task status
   */
  changeStatus: async (id: string, status: string): Promise<Task> => {
    const response = await apiClient.post<Task>(`/tasks/${id}/status`, { status })
    return response as any
  },

  /**
   * Change task priority
   */
  changePriority: async (id: string, priority: string): Promise<Task> => {
    const response = await apiClient.post<Task>(`/tasks/${id}/priority`, { priority })
    return response as any
  },

  /**
   * Assign task to user
   */
  assign: async (id: string, userId: string): Promise<Task> => {
    const response = await apiClient.post<Task>(`/tasks/${id}/assign`, { assignedToId: userId })
    return response as any
  },

  /**
   * Complete task
   */
  complete: async (id: string): Promise<Task> => {
    const response = await apiClient.post<Task>(`/tasks/${id}/complete`)
    return response as any
  },

  /**
   * Cancel task
   */
  cancel: async (id: string, reason?: string): Promise<Task> => {
    const endpoint = reason 
      ? `/tasks/${id}/cancel?reason=${encodeURIComponent(reason)}`
      : `/tasks/${id}/cancel`
    const response = await apiClient.post<Task>(endpoint)
    return response as any
  },

  /**
   * Get task comments
   */
  getComments: async (taskId: string): Promise<TaskComment[]> => {
    const response = await apiClient.get<TaskComment[]>(`/tasks/${taskId}/comments`)
    return response as any
  },

  /**
   * Add task comment
   */
  addComment: async (data: CreateTaskCommentRequest): Promise<TaskComment> => {
    const response = await apiClient.post<TaskComment>(`/tasks/${data.taskId}/comments`, data)
    return response as any
  },

  /**
   * Delete task comment
   */
  deleteComment: async (taskId: string, commentId: string): Promise<void> => {
    await apiClient.delete(`/tasks/${taskId}/comments/${commentId}`)
  },

  /**
   * Get task statistics
   */
  getStats: async (): Promise<TaskStats> => {
    const response = await apiClient.get<TaskStats>('/tasks/stats')
    return response as any
  },

  /**
   * Search tasks
   */
  search: async (keyword: string): Promise<Task[]> => {
    const response = await apiClient.get<Task[]>('/tasks/search', { keyword })
    return response as any
  },
}
