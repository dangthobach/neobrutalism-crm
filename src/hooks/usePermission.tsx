'use client'

import React from 'react'
import { useAuth } from '@/contexts/auth-context'
import { useQuery } from '@tanstack/react-query'
import { userApi } from '@/lib/api/users'

/**
 * Permission action types matching backend PermissionType
 */
export type PermissionAction = 'READ' | 'WRITE' | 'DELETE' | 'EXECUTE'

/**
 * Menu permission types from RoleMenu
 */
export type MenuPermission = 'canView' | 'canCreate' | 'canEdit' | 'canDelete' | 'canExport' | 'canImport'

interface MenuPermissions {
  canView?: boolean
  canCreate?: boolean
  canEdit?: boolean
  canDelete?: boolean
  canExport?: boolean
  canImport?: boolean
}

interface UserMenu {
  id: string
  code: string
  name: string
  route?: string
  permissions?: MenuPermissions
  children?: UserMenu[]
}

/**
 * Hook to check user permissions for specific screens and actions
 * Automatically checks both menu-level and API-level permissions
 *
 * @example
 * const { hasPermission, canView, canCreate, canEdit, canDelete } = usePermission()
 *
 * // Check specific permission
 * if (hasPermission('/users', 'canCreate')) {
 *   // Show create button
 * }
 *
 * // Use convenience methods
 * if (canCreate('/users')) {
 *   // Show create button
 * }
 */
export function usePermission() {
  const { user, isAuthenticated } = useAuth()

  // Fetch user's menu tree with permissions from API
  const { data: userMenus = [] } = useQuery<UserMenu[]>({
    queryKey: ['userMenus', user?.id],
    queryFn: () => userApi.getCurrentUserMenus(),
    enabled: isAuthenticated && !!user,
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
    retry: 1,
  })

  /**
   * Find menu by route or code
   */
  const findMenu = (routeOrCode: string): UserMenu | undefined => {
    const findRecursive = (menus: UserMenu[]): UserMenu | undefined => {
      for (const menu of menus) {
        if (menu.route === routeOrCode || menu.code === routeOrCode) {
          return menu
        }
        if (menu.children) {
          const found = findRecursive(menu.children)
          if (found) return found
        }
      }
      return undefined
    }

    return findRecursive(userMenus)
  }

  /**
   * Check if user has specific menu permission
   *
   * @param routeOrCode - Menu route (e.g., '/users') or code (e.g., 'USERS')
   * @param permission - Menu permission to check
   * @returns true if user has the permission
   */
  const hasMenuPermission = (routeOrCode: string, permission: MenuPermission): boolean => {
    const menu = findMenu(routeOrCode)
    if (!menu || !menu.permissions) {
      return false
    }

    return menu.permissions[permission] === true
  }

  /**
   * Check if user has permission for specific action
   * This checks menu-level permissions based on action type
   *
   * @param routeOrCode - Menu route or code
   * @param action - Permission action type
   * @returns true if user has permission
   */
  const hasPermission = (routeOrCode: string, action: PermissionAction | MenuPermission): boolean => {
    // If it's already a menu permission, check directly
    if (['canView', 'canCreate', 'canEdit', 'canDelete', 'canExport', 'canImport'].includes(action)) {
      return hasMenuPermission(routeOrCode, action as MenuPermission)
    }

    // Map API permission types to menu permissions
    switch (action) {
      case 'READ':
        return hasMenuPermission(routeOrCode, 'canView')
      case 'WRITE':
        // WRITE requires either canCreate or canEdit
        return hasMenuPermission(routeOrCode, 'canCreate') ||
               hasMenuPermission(routeOrCode, 'canEdit')
      case 'DELETE':
        return hasMenuPermission(routeOrCode, 'canDelete')
      case 'EXECUTE':
        // EXECUTE is similar to WRITE
        return hasMenuPermission(routeOrCode, 'canCreate') ||
               hasMenuPermission(routeOrCode, 'canEdit')
      default:
        return false
    }
  }

  /**
   * Convenience method to check view permission
   */
  const canView = (routeOrCode: string): boolean => {
    return hasMenuPermission(routeOrCode, 'canView')
  }

  /**
   * Convenience method to check create permission
   */
  const canCreate = (routeOrCode: string): boolean => {
    return hasMenuPermission(routeOrCode, 'canCreate')
  }

  /**
   * Convenience method to check edit permission
   */
  const canEdit = (routeOrCode: string): boolean => {
    return hasMenuPermission(routeOrCode, 'canEdit')
  }

  /**
   * Convenience method to check delete permission
   */
  const canDelete = (routeOrCode: string): boolean => {
    return hasMenuPermission(routeOrCode, 'canDelete')
  }

  /**
   * Convenience method to check export permission
   */
  const canExport = (routeOrCode: string): boolean => {
    return hasMenuPermission(routeOrCode, 'canExport')
  }

  /**
   * Convenience method to check import permission
   */
  const canImport = (routeOrCode: string): boolean => {
    return hasMenuPermission(routeOrCode, 'canImport')
  }

  /**
   * Check multiple permissions at once
   *
   * @param routeOrCode - Menu route or code
   * @param permissions - Array of permissions to check
   * @returns Object with permission results
   */
  const checkPermissions = (
    routeOrCode: string,
    permissions: (PermissionAction | MenuPermission)[]
  ): Record<string, boolean> => {
    const results: Record<string, boolean> = {}
    for (const permission of permissions) {
      results[permission] = hasPermission(routeOrCode, permission)
    }
    return results
  }

  /**
   * Get all permissions for a route
   */
  const getPermissions = (routeOrCode: string): MenuPermissions => {
    const menu = findMenu(routeOrCode)
    return menu?.permissions || {}
  }

  return {
    hasPermission,
    hasMenuPermission,
    canView,
    canCreate,
    canEdit,
    canDelete,
    canExport,
    canImport,
    checkPermissions,
    getPermissions,
    userMenus,
  }
}

/**
 * HOC to protect components based on permissions
 *
 * @example
 * const ProtectedButton = withPermission(Button, '/users', 'canCreate')
 */
export function withPermission<P extends object>(
  Component: React.ComponentType<P>,
  routeOrCode: string,
  permission: PermissionAction | MenuPermission
) {
  return function PermissionWrappedComponent(props: P) {
    const { hasPermission } = usePermission()

    if (!hasPermission(routeOrCode, permission)) {
      return null
    }

    return <Component {...props} />
  }
}
