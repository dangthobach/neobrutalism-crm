/**
 * Menu Synchronization Utility
 * Syncs hardcoded UI menus to database
 */

import { menuApi, CreateMenuRequest } from './api/menus'
import { menuTabApi, CreateMenuTabRequest } from './api/menu-tabs'
import { menuScreenApi, CreateMenuScreenRequest } from './api/menu-screens'

/**
 * UI Menu Definition
 */
export interface UIMenuItem {
  code: string
  name: string
  icon?: string
  route?: string
  description?: string
  children?: UIMenuItem[]
  displayOrder?: number
}

/**
 * Current UI Menu Structure from admin-sidebar.tsx
 * Exactly matches the current admin sidebar
 */
const UI_MENUS: UIMenuItem[] = [
  {
    code: 'USER_MANAGEMENT',
    name: 'User Management',
    icon: '👥',
    displayOrder: 1,
    children: [
      {
        code: 'USERS',
        name: 'Users',
        icon: '👤',
        route: '/admin/users',
        description: 'Manage user accounts',
        displayOrder: 1,
      },
      {
        code: 'ROLES',
        name: 'Roles',
        icon: '🛡️',
        route: '/admin/roles',
        description: 'Manage roles & permissions',
        displayOrder: 2,
      },
      {
        code: 'GROUPS',
        name: 'Groups',
        icon: '👥',
        route: '/admin/groups',
        description: 'Manage user groups',
        displayOrder: 3,
      },
      {
        code: 'ORGANIZATIONS',
        name: 'Organizations',
        icon: '🏢',
        route: '/admin/organizations',
        description: 'Manage organizations',
        displayOrder: 4,
      },
    ],
  },
  {
    code: 'PERMISSION_SYSTEM',
    name: 'Permission System',
    icon: '🔐',
    displayOrder: 2,
    children: [
      {
        code: 'USER_PERMISSIONS',
        name: 'User Permissions',
        icon: '🔑',
        route: '/admin/permissions/users',
        description: 'Assign permissions to users',
        displayOrder: 1,
      },
      {
        code: 'ROLE_PERMISSIONS',
        name: 'Role Permissions',
        icon: '🛡️',
        route: '/admin/permissions/roles',
        description: 'Configure role permissions',
        displayOrder: 2,
      },
    ],
  },
  {
    code: 'MENU_MANAGEMENT',
    name: 'Menu Management',
    icon: '📋',
    displayOrder: 3,
    children: [
      {
        code: 'MENUS',
        name: 'Menus',
        icon: '📋',
        route: '/admin/menus',
        description: 'Manage menu structure',
        displayOrder: 1,
      },
      {
        code: 'MENU_TABS',
        name: 'Menu Tabs',
        icon: '📑',
        route: '/admin/menu-tabs',
        description: 'Manage menu tabs',
        displayOrder: 2,
      },
      {
        code: 'MENU_SCREENS',
        name: 'Menu Screens',
        icon: '🖥️',
        route: '/admin/menu-screens',
        description: 'Manage menu screens',
        displayOrder: 3,
      },
      {
        code: 'API_ENDPOINTS',
        name: 'API Endpoints',
        icon: '🔌',
        route: '/admin/api-endpoints',
        description: 'Manage API endpoints',
        displayOrder: 4,
      },
    ],
  },
]

/**
 * Sync result interface
 */
export interface SyncResult {
  success: boolean
  menusCreated: number
  menusUpdated: number
  menusSkipped: number
  errors: string[]
}

/**
 * Recursively sync UI menus to database
 */
async function syncMenuRecursive(
  menuItem: UIMenuItem,
  parentId?: string,
  result: SyncResult = { success: true, menusCreated: 0, menusUpdated: 0, menusSkipped: 0, errors: [] }
): Promise<string | undefined> {
  try {
    // Check if menu already exists
    let menuId: string | undefined
    try {
      const existingMenu = await menuApi.getMenuByCode(menuItem.code)
      menuId = existingMenu.id
      result.menusSkipped++
      console.log(`Menu ${menuItem.code} already exists, skipping...`)
    } catch (error) {
      // Menu doesn't exist, create it
      const menuData: Partial<CreateMenuRequest> = {
        code: menuItem.code,
        name: menuItem.name,
        icon: menuItem.icon,
        route: menuItem.route,
        parentId,
        displayOrder: menuItem.displayOrder || 0,
        isVisible: true,
        requiresAuth: true,
      }

      const createdMenu = await menuApi.createMenu(menuData)
      menuId = createdMenu.id
      result.menusCreated++
      console.log(`Created menu: ${menuItem.code}`)
    }

    // Sync children recursively
    if (menuItem.children && menuItem.children.length > 0) {
      for (const child of menuItem.children) {
        await syncMenuRecursive(child, menuId, result)
      }
    }

    return menuId
  } catch (error: any) {
    const errorMsg = `Error syncing menu ${menuItem.code}: ${error.message}`
    result.errors.push(errorMsg)
    console.error(errorMsg)
    return undefined
  }
}

/**
 * Sync all UI menus to database
 */
export async function syncUIMenusToDatabase(): Promise<SyncResult> {
  console.log('Starting menu synchronization...')

  const result: SyncResult = {
    success: true,
    menusCreated: 0,
    menusUpdated: 0,
    menusSkipped: 0,
    errors: [],
  }

  try {
    for (const rootMenu of UI_MENUS) {
      await syncMenuRecursive(rootMenu, undefined, result)
    }

    result.success = result.errors.length === 0

    console.log('Menu synchronization completed:', result)
    return result
  } catch (error: any) {
    result.success = false
    result.errors.push(`Fatal error: ${error.message}`)
    console.error('Menu synchronization failed:', error)
    return result
  }
}

/**
 * Get UI menu structure for preview
 */
export function getUIMenuStructure(): UIMenuItem[] {
  return UI_MENUS
}

/**
 * Count total menus in UI structure
 */
export function countUIMenus(): number {
  let count = 0

  function countRecursive(items: UIMenuItem[]) {
    items.forEach(item => {
      count++
      if (item.children) {
        countRecursive(item.children)
      }
    })
  }

  countRecursive(UI_MENUS)
  return count
}
