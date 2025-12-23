"use client"

import { usePermission, PermissionAction, MenuPermission } from '@/hooks/usePermission'
import { ReactNode } from 'react'

interface PermissionGuardProps {
  children: ReactNode
  routeOrCode: string
  permission?: PermissionAction | MenuPermission
  fallback?: ReactNode
}

/**
 * Component to conditionally render children based on permissions
 * Use this to show/hide UI elements based on user permissions
 * 
 * @example
 * <PermissionGuard routeOrCode="/users" permission="canCreate">
 *   <Button>Create User</Button>
 * </PermissionGuard>
 */
export function PermissionGuard({
  children,
  routeOrCode,
  permission,
  fallback = null,
}: PermissionGuardProps) {
  const { hasPermission, canView } = usePermission()

  // If no specific permission provided, check view permission
  const hasAccess = permission 
    ? hasPermission(routeOrCode, permission)
    : canView(routeOrCode)

  if (!hasAccess) {
    return <>{fallback}</>
  }

  return <>{children}</>
}
