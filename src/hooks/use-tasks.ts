/**
 * Task React Query Hooks
 * Provides data fetching and mutations for tasks
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { taskApi } from '@/lib/api/tasks'
import type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskSearchParams,
  CreateTaskCommentRequest,
} from '@/types/task'
import { toast } from 'sonner'

// Query keys
export const taskKeys = {
  all: ['tasks'] as const,
  lists: () => [...taskKeys.all, 'list'] as const,
  list: (params?: TaskSearchParams) => [...taskKeys.lists(), params] as const,
  details: () => [...taskKeys.all, 'detail'] as const,
  detail: (id: string) => [...taskKeys.details(), id] as const,
  byCustomer: (customerId: string) => [...taskKeys.all, 'customer', customerId] as const,
  byContact: (contactId: string) => [...taskKeys.all, 'contact', contactId] as const,
  byAssignee: (userId: string) => [...taskKeys.all, 'assignee', userId] as const,
  board: () => [...taskKeys.all, 'board'] as const,
  myTasks: () => [...taskKeys.all, 'my-tasks'] as const,
  overdue: () => [...taskKeys.all, 'overdue'] as const,
  dueToday: () => [...taskKeys.all, 'due-today'] as const,
  dueThisWeek: () => [...taskKeys.all, 'due-this-week'] as const,
  stats: () => [...taskKeys.all, 'stats'] as const,
  search: (keyword: string) => [...taskKeys.all, 'search', keyword] as const,
  comments: (taskId: string) => [...taskKeys.all, 'comments', taskId] as const,
}

/**
 * Get all tasks with pagination
 */
export function useTasks(params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.list(params),
    queryFn: () => taskApi.getAll(params),
  })
}

/**
 * Get task by ID
 */
export function useTask(id: string | undefined) {
  return useQuery({
    queryKey: taskKeys.detail(id!),
    queryFn: () => taskApi.getById(id!),
    enabled: !!id,
  })
}

/**
 * Get tasks by customer
 */
export function useTasksByCustomer(customerId: string, params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.byCustomer(customerId),
    queryFn: () => taskApi.getByCustomer(customerId, params),
    enabled: !!customerId,
  })
}

/**
 * Get tasks by contact
 */
export function useTasksByContact(contactId: string, params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.byContact(contactId),
    queryFn: () => taskApi.getByContact(contactId, params),
    enabled: !!contactId,
  })
}

/**
 * Get tasks assigned to user
 */
export function useTasksByAssignee(userId: string, params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.byAssignee(userId),
    queryFn: () => taskApi.getByAssignee(userId, params),
    enabled: !!userId,
  })
}

/**
 * Get task board (Kanban view)
 */
export function useTaskBoard(params?: Omit<TaskSearchParams, 'status'>) {
  return useQuery({
    queryKey: taskKeys.board(),
    queryFn: () => taskApi.getBoard(params),
  })
}

/**
 * Get my tasks
 */
export function useMyTasks(params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.myTasks(),
    queryFn: () => taskApi.getMyTasks(params),
  })
}

/**
 * Get overdue tasks
 */
export function useOverdueTasks(params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.overdue(),
    queryFn: () => taskApi.getOverdue(params),
  })
}

/**
 * Get tasks due today
 */
export function useDueTodayTasks(params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.dueToday(),
    queryFn: () => taskApi.getDueToday(params),
  })
}

/**
 * Get tasks due this week
 */
export function useDueThisWeekTasks(params?: TaskSearchParams) {
  return useQuery({
    queryKey: taskKeys.dueThisWeek(),
    queryFn: () => taskApi.getDueThisWeek(params),
  })
}

/**
 * Get task statistics
 */
export function useTaskStats() {
  return useQuery({
    queryKey: taskKeys.stats(),
    queryFn: () => taskApi.getStats(),
  })
}

/**
 * Search tasks
 */
export function useTaskSearch(keyword: string) {
  return useQuery({
    queryKey: taskKeys.search(keyword),
    queryFn: () => taskApi.search(keyword),
    enabled: keyword.length > 0,
  })
}

/**
 * Get task comments
 */
export function useTaskComments(taskId: string) {
  return useQuery({
    queryKey: taskKeys.comments(taskId),
    queryFn: () => taskApi.getComments(taskId),
    enabled: !!taskId,
  })
}

/**
 * Create task
 */
export function useCreateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateTaskRequest) => taskApi.create(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      queryClient.invalidateQueries({ queryKey: taskKeys.stats() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: taskKeys.byCustomer(data.customerId) })
      }
      if (data.contactId) {
        queryClient.invalidateQueries({ queryKey: taskKeys.byContact(data.contactId) })
      }
      if (data.assignedToId) {
        queryClient.invalidateQueries({ queryKey: taskKeys.byAssignee(data.assignedToId) })
      }
      toast.success('Task created successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create task')
    },
  })
}

/**
 * Update task
 */
export function useUpdateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTaskRequest }) =>
      taskApi.update(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      queryClient.invalidateQueries({ queryKey: taskKeys.stats() })
      if (data.customerId) {
        queryClient.invalidateQueries({ queryKey: taskKeys.byCustomer(data.customerId) })
      }
      if (data.contactId) {
        queryClient.invalidateQueries({ queryKey: taskKeys.byContact(data.contactId) })
      }
      toast.success('Task updated successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update task')
    },
  })
}

/**
 * Delete task
 */
export function useDeleteTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => taskApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      queryClient.invalidateQueries({ queryKey: taskKeys.stats() })
      queryClient.invalidateQueries({ queryKey: taskKeys.all })
      toast.success('Task deleted successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete task')
    },
  })
}

/**
 * Change task status
 */
export function useChangeTaskStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      taskApi.changeStatus(id, status),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      queryClient.invalidateQueries({ queryKey: taskKeys.stats() })
      toast.success('Task status updated')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update task status')
    },
  })
}

/**
 * Change task priority
 */
export function useChangeTaskPriority() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, priority }: { id: string; priority: string }) =>
      taskApi.changePriority(id, priority),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      toast.success('Task priority updated')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update task priority')
    },
  })
}

/**
 * Assign task to user
 */
export function useAssignTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, userId }: { id: string; userId: string }) =>
      taskApi.assign(id, userId),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      if (data.assignedToId) {
        queryClient.invalidateQueries({ queryKey: taskKeys.byAssignee(data.assignedToId) })
      }
      toast.success('Task assigned successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to assign task')
    },
  })
}

/**
 * Complete task
 */
export function useCompleteTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => taskApi.complete(id),
    onSuccess: (data, id) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.detail(id) })
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      queryClient.invalidateQueries({ queryKey: taskKeys.stats() })
      toast.success('Task marked as completed')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to complete task')
    },
  })
}

/**
 * Cancel task
 */
export function useCancelTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      taskApi.cancel(id, reason),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.detail(variables.id) })
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      queryClient.invalidateQueries({ queryKey: taskKeys.board() })
      queryClient.invalidateQueries({ queryKey: taskKeys.stats() })
      toast.success('Task cancelled')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to cancel task')
    },
  })
}

/**
 * Add task comment
 */
export function useAddTaskComment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateTaskCommentRequest) => taskApi.addComment(data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.comments(variables.taskId) })
      toast.success('Comment added successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to add comment')
    },
  })
}

/**
 * Delete task comment
 */
export function useDeleteTaskComment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ taskId, commentId }: { taskId: string; commentId: string }) =>
      taskApi.deleteComment(taskId, commentId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.comments(variables.taskId) })
      toast.success('Comment deleted successfully')
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete comment')
    },
  })
}
