/**
 * React Query hooks for User management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi, User, CreateUserRequest, UpdateUserRequest, UserQueryParams, UserStatus } from '@/lib/api/users'
import { ApiError } from '@/lib/api/client'
import { toast } from 'sonner'

const USERS_QUERY_KEY = 'users'

/**
 * Fetch users with pagination
 */
export function useUsers(params?: UserQueryParams) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, params],
    queryFn: () => userApi.getUsers(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Fetch user by ID
 */
export function useUser(id: string) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, id],
    queryFn: () => userApi.getUserById(id),
    enabled: !!id,
  })
}

/**
 * Fetch users by organization
 */
export function useUsersByOrganization(organizationId: string) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, 'organization', organizationId],
    queryFn: () => userApi.getUsersByOrganization(organizationId),
    enabled: !!organizationId,
  })
}

/**
 * Fetch users by status
 */
export function useUsersByStatus(status: UserStatus) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, 'status', status],
    queryFn: () => userApi.getUsersByStatus(status),
  })
}

/**
 * Create new user
 */
export function useCreateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateUserRequest) => userApi.createUser(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User created successfully', {
        description: `${data.fullName} has been created.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to create user', {
        description: error.message,
      })
    },
  })
}

/**
 * Update user
 */
export function useUpdateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      userApi.updateUser(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY, variables.id] })
      toast.success('User updated successfully', {
        description: `${data.fullName} has been updated.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to update user', {
        description: error.message,
      })
    },
  })
}

/**
 * Delete user
 */
export function useDeleteUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => userApi.deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User deleted successfully')
    },
    onError: (error: ApiError) => {
      toast.error('Failed to delete user', {
        description: error.message,
      })
    },
  })
}

/**
 * Activate user
 */
export function useActivateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      userApi.activateUser(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User activated', {
        description: `${data.fullName} is now active.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to activate user', {
        description: error.message,
      })
    },
  })
}

/**
 * Suspend user
 */
export function useSuspendUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      userApi.suspendUser(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User suspended', {
        description: `${data.fullName} has been suspended.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to suspend user', {
        description: error.message,
      })
    },
  })
}

/**
 * Lock user
 */
export function useLockUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      userApi.lockUser(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User locked', {
        description: `${data.fullName} has been locked.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to lock user', {
        description: error.message,
      })
    },
  })
}

/**
 * Unlock user
 */
export function useUnlockUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      userApi.unlockUser(id, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
      toast.success('User unlocked', {
        description: `${data.fullName} has been unlocked.`,
      })
    },
    onError: (error: ApiError) => {
      toast.error('Failed to unlock user', {
        description: error.message,
      })
    },
  })
}

/**
 * Check username availability
 */
export function useCheckUsername() {
  return useMutation({
    mutationFn: (username: string) => userApi.checkUsername(username),
  })
}

/**
 * Check email availability
 */
export function useCheckEmail() {
  return useMutation({
    mutationFn: (email: string) => userApi.checkEmail(email),
  })
}
