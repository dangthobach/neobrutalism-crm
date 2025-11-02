import { ReactNode } from 'react'
import { usePermission, PermissionAction, MenuPermission } from '@/hooks/usePermission'

interface PermissionGuardProps {
  /**
   * Menu route or code to check permission against
   */
  route: string

  /**
   * Permission required to show children
   */
  permission: PermissionAction | MenuPermission

  /**
   * Content to show when user has permission
   */
  children: ReactNode

  /**
   * Optional fallback content when user doesn't have permission
   */
  fallback?: ReactNode
}

/**
 * Permission guard component
 * Only renders children if user has the required permission
 *
 * @example
 * <PermissionGuard route="/users" permission="canCreate">
 *   <Button>Create User</Button>
 * </PermissionGuard>
 */
export function PermissionGuard({
  route,
  permission,
  children,
  fallback = null,
}: PermissionGuardProps) {
  const { hasPermission } = usePermission()

  if (!hasPermission(route, permission)) {
    return <>{fallback}</>
  }

  return <>{children}</>
}

/**
 * Multiple permission guard - shows content if user has ALL specified permissions
 *
 * @example
 * <PermissionGuardAll route="/users" permissions={['canView', 'canCreate']}>
 *   <UserManagement />
 * </PermissionGuardAll>
 */
export function PermissionGuardAll({
  route,
  permissions,
  children,
  fallback = null,
}: {
  route: string
  permissions: (PermissionAction | MenuPermission)[]
  children: ReactNode
  fallback?: ReactNode
}) {
  const { hasPermission } = usePermission()

  const hasAllPermissions = permissions.every((permission) =>
    hasPermission(route, permission)
  )

  if (!hasAllPermissions) {
    return <>{fallback}</>
  }

  return <>{children}</>
}

/**
 * Multiple permission guard - shows content if user has ANY of the specified permissions
 *
 * @example
 * <PermissionGuardAny route="/users" permissions={['canCreate', 'canEdit']}>
 *   <Button>Modify</Button>
 * </PermissionGuardAny>
 */
export function PermissionGuardAny({
  route,
  permissions,
  children,
  fallback = null,
}: {
  route: string
  permissions: (PermissionAction | MenuPermission)[]
  children: ReactNode
  fallback?: ReactNode
}) {
  const { hasPermission } = usePermission()

  const hasAnyPermission = permissions.some((permission) =>
    hasPermission(route, permission)
  )

  if (!hasAnyPermission) {
    return <>{fallback}</>
  }

  return <>{children}</>
}
