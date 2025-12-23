import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'

interface CurrentUser {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  phone?: string
  avatar?: string
  organizationId: string
  branchId?: string
  dataScope: 'ALL_BRANCHES' | 'CURRENT_BRANCH' | 'SELF_ONLY'
  roles: string[]
  permissions: string[]
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'LOCKED' | 'INACTIVE'
}

/**
 * Get current authenticated user info from backend
 */
export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiClient.get<CurrentUser>('/auth/me')
  return response as CurrentUser
}

/**
 * React Query hook to get current user
 * Caches for 5 minutes
 */
export function useCurrentUser() {
  return useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUser,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
    retry: 1,
    refetchOnWindowFocus: false,
  })
}

/**
 * Get just the organization ID of current user
 */
export function useCurrentOrganization() {
  const { data: user } = useCurrentUser()
  return user?.organizationId
}

/**
 * Get just the user ID of current user
 */
export function useCurrentUserId() {
  const { data: user } = useCurrentUser()
  return user?.id
}

/**
 * Check if current user has a specific role
 */
export function useHasRole(role: string) {
  const { data: user } = useCurrentUser()
  return user?.roles?.includes(role) ?? false
}

/**
 * Check if current user has a specific permission
 */
export function useHasPermission(permission: string) {
  const { data: user } = useCurrentUser()
  return user?.permissions?.includes(permission) ?? false
}
