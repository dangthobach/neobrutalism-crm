import {
  User,
  UserStatus,
  Role,
  RoleStatus,
  Group,
  Permission,
  RolePermission,
  MenuTreeNode,
  Menu,
  MenuTab,
  MenuScreen,
  PermissionMatrix,
  UserWithRoles,
} from '@/types/permission'

// --- Mock reference data ---

export const mockRoles: Role[] = [
  {
    id: 'role_1',
    code: 'ROLE_ADMIN',
    name: 'Administrator',
    description: 'Full system access with all permissions',
    organizationId: 'org_1',
    isSystem: true,
    priority: 100,
    status: RoleStatus.ACTIVE,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  },
  {
    id: 'role_2',
    code: 'ROLE_MANAGER',
    name: 'Manager',
    description: 'Manager access with elevated permissions',
    organizationId: 'org_1',
    isSystem: false,
    priority: 50,
    status: RoleStatus.ACTIVE,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  },
  {
    id: 'role_3',
    code: 'ROLE_USER',
    name: 'Standard User',
    description: 'Basic access with read permissions',
    organizationId: 'org_1',
    isSystem: false,
    priority: 10,
    status: RoleStatus.ACTIVE,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  },
]

export const mockGroups: Group[] = [
  {
    id: 'grp_1',
    code: 'WH_NORTH',
    name: 'Warehouse North',
    description: 'Northern region operations',
    parentId: undefined,
    organizationId: 'org_1',
    level: 1,
    path: '/WH_NORTH',
    status: 1 ? RoleStatus.ACTIVE : RoleStatus.ACTIVE, // type reuse
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  } as unknown as Group, // simplify status enum reuse
  {
    id: 'grp_2',
    code: 'WH_SOUTH',
    name: 'Warehouse South',
    description: 'Southern region operations',
    parentId: undefined,
    organizationId: 'org_1',
    level: 1,
    path: '/WH_SOUTH',
    status: 1 ? RoleStatus.ACTIVE : RoleStatus.ACTIVE,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  } as unknown as Group,
]

export const mockUsers: User[] = [
  {
    id: 'u_1',
    username: 'jdoe',
    email: 'jdoe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '+1-555-0001',
    avatar: undefined,
    organizationId: 'org_1',
    lastLoginAt: new Date(Date.now() - 3600_000).toISOString(),
    lastLoginIp: '10.0.0.1',
    passwordChangedAt: new Date(Date.now() - 86_400_000).toISOString(),
    failedLoginAttempts: 0,
    lockedUntil: undefined,
    status: UserStatus.ACTIVE,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  },
  {
    id: 'u_2',
    username: 'asmith',
    email: 'asmith@example.com',
    firstName: 'Alice',
    lastName: 'Smith',
    phone: '+1-555-0002',
    avatar: undefined,
    organizationId: 'org_1',
    lastLoginAt: new Date(Date.now() - 7200_000).toISOString(),
    lastLoginIp: '10.0.0.2',
    passwordChangedAt: new Date(Date.now() - 172_800_000).toISOString(),
    failedLoginAttempts: 1,
    lockedUntil: undefined,
    status: UserStatus.PENDING,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  },
  {
    id: 'u_3',
    username: 'bwayne',
    email: 'bwayne@example.com',
    firstName: 'Bruce',
    lastName: 'Wayne',
    phone: '+1-555-0003',
    avatar: undefined,
    organizationId: 'org_1',
    lastLoginAt: undefined,
    lastLoginIp: undefined,
    passwordChangedAt: undefined,
    failedLoginAttempts: 0,
    lockedUntil: undefined,
    status: UserStatus.SUSPENDED,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false,
  },
]

// Menu structure mock
const menus: Menu[] = [
  {
    id: 'm_users',
    code: 'MENU_USERS',
    name: 'Users',
    icon: 'users',
    parentId: undefined,
    level: 1,
    path: '/MENU_USERS',
    route: '/admin/permissions/users',
    displayOrder: 1,
    isVisible: true,
    requiresAuth: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    deleted: false,
  },
  {
    id: 'm_roles',
    code: 'MENU_ROLES',
    name: 'Roles',
    icon: 'shield',
    parentId: undefined,
    level: 1,
    path: '/MENU_ROLES',
    route: '/admin/permissions/roles',
    displayOrder: 2,
    isVisible: true,
    requiresAuth: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    deleted: false,
  },
  {
    id: 'm_perms',
    code: 'MENU_PERMISSIONS',
    name: 'Permissions',
    icon: 'key',
    parentId: undefined,
    level: 1,
    path: '/MENU_PERMISSIONS',
    route: '/admin/permissions/overview',
    displayOrder: 3,
    isVisible: true,
    requiresAuth: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    deleted: false,
  },
  {
    id: 'm_debt',
    code: 'MENU_DEBT_SALE',
    name: 'Debt Sale',
    icon: 'coins',
    parentId: undefined,
    level: 1,
    path: '/MENU_DEBT_SALE',
    route: '/admin/debt-sale',
    displayOrder: 4,
    isVisible: true,
    requiresAuth: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    deleted: false,
  },
]

export const mockMenuTabs: MenuTab[] = [
  { id: 'tab_1', code: 'USERS_GENERAL', name: 'General', menuId: 'm_users', icon: 'info', displayOrder: 1, isVisible: true, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
]

export const mockMenuScreens: MenuScreen[] = [
  { id: 'scr_users', code: 'USERS', name: 'Users List', menuId: 'm_users', tabId: undefined, route: '/admin/permissions/users', component: 'UsersPage', requiresPermission: true, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
  { id: 'scr_roles', code: 'ROLES', name: 'Roles Matrix', menuId: 'm_roles', tabId: undefined, route: '/admin/permissions/roles', component: 'RolesPage', requiresPermission: true, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
  { id: 'scr_perm_audit', code: 'PERM_AUDIT', name: 'Permission Audit', menuId: 'm_perms', tabId: undefined, route: '/admin/permissions/audit', component: 'PermissionAuditPage', requiresPermission: true, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
  { id: 'scr_debt', code: 'DEBT', name: 'Debt Sale', menuId: 'm_debt', tabId: undefined, route: '/admin/debt-sale', component: 'DebtSale', requiresPermission: true, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
]

export function buildMenuTree(): MenuTreeNode[] {
  const nodes: MenuTreeNode[] = menus
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((m) => ({ ...m, children: [], tabs: mockMenuTabs.filter((t) => t.menuId === m.id) }))
  return nodes
}

// --- Permissions ---

export const mockPermissions: Permission[] = [
  { id: 'p_1', featureId: 'USERS', actionCode: 'USER_VIEW', actionLabel: 'View Users', description: 'Can view user list', riskLevel: 'LOW' },
  { id: 'p_2', featureId: 'USERS', actionCode: 'USER_CREATE', actionLabel: 'Create User', description: 'Can create users', requires: ['USER_VIEW'], riskLevel: 'MEDIUM' },
  { id: 'p_3', featureId: 'USERS', actionCode: 'USER_EDIT', actionLabel: 'Edit User', description: 'Can edit users', requires: ['USER_VIEW'], riskLevel: 'MEDIUM' },
  { id: 'p_4', featureId: 'USERS', actionCode: 'USER_DELETE', actionLabel: 'Delete User', description: 'Can delete users', requires: ['USER_VIEW'], riskLevel: 'HIGH' },

  { id: 'p_5', featureId: 'ROLES', actionCode: 'ROLE_VIEW', actionLabel: 'View Roles', riskLevel: 'LOW' },
  { id: 'p_6', featureId: 'ROLES', actionCode: 'ROLE_MANAGE', actionLabel: 'Manage Roles', requires: ['ROLE_VIEW'], riskLevel: 'HIGH' },

  { id: 'p_7', featureId: 'PERM_AUDIT', actionCode: 'PERMISSION_VIEW', actionLabel: 'View Permissions', riskLevel: 'LOW' },
  { id: 'p_8', featureId: 'PERM_AUDIT', actionCode: 'PERMISSION_MANAGE', actionLabel: 'Manage Permissions', requires: ['PERMISSION_VIEW'], riskLevel: 'HIGH' },

  { id: 'p_9', featureId: 'DEBT', actionCode: 'DEBT_VIEW', actionLabel: 'View Debt', riskLevel: 'MEDIUM' },
  { id: 'p_10', featureId: 'DEBT', actionCode: 'DEBT_APPROVE', actionLabel: 'Approve Debt', requires: ['DEBT_VIEW'], riskLevel: 'CRITICAL' },
]

// Role -> permission codes
export const mockRolePermissions: Map<string, string[]> = new Map([
  ['ROLE_ADMIN', mockPermissions.map((p) => p.actionCode)],
  ['ROLE_MANAGER', ['USER_VIEW', 'USER_EDIT', 'ROLE_VIEW', 'PERMISSION_VIEW', 'DEBT_VIEW']],
  ['ROLE_USER', ['USER_VIEW']],
])

// Role -> visible menu codes
export const mockRoleMenus: Map<string, string[]> = new Map([
  ['ROLE_ADMIN', ['MENU_USERS', 'MENU_ROLES', 'MENU_PERMISSIONS', 'MENU_DEBT_SALE']],
  ['ROLE_MANAGER', ['MENU_USERS', 'MENU_ROLES', 'MENU_PERMISSIONS']], // intentionally omits MENU_DEBT_SALE to trigger warning
  ['ROLE_USER', ['MENU_USERS']],
])

// User -> role codes
export const mockUserRoles: Map<string, string[]> = new Map([
  ['u_1', ['ROLE_ADMIN']],
  ['u_2', ['ROLE_MANAGER']],
  ['u_3', ['ROLE_USER']],
])

// --- Helpers ---

export function getPermissionMatrixForRole(roleCode: string): PermissionMatrix[] {
  const granted = new Set(mockRolePermissions.get(roleCode) || [])
  const byFeature = new Map<string, Permission[]>()
  for (const p of mockPermissions) {
    const arr = byFeature.get(p.featureId) || []
    arr.push(p)
    byFeature.set(p.featureId, arr)
  }

  const matrix: PermissionMatrix[] = []
  byFeature.forEach((perms, featureId) => {
    const screen = mockMenuScreens.find((s) => s.code === featureId)
    matrix.push({
      featureId,
      featureName: screen?.name || featureId,
      actions: perms.map((permission) => ({ permission, granted: granted.has(permission.actionCode) })),
    })
  })
  return matrix
}

export function getUserWithRoles(userId: string): UserWithRoles | null {
  const user = mockUsers.find((u) => u.id === userId)
  if (!user) return null
  const roleCodes = mockUserRoles.get(userId) || []
  const roles = mockRoles.filter((r) => roleCodes.includes(r.code))

  // Effective menus
  const menuCodes = new Set<string>()
  for (const rc of roleCodes) {
    (mockRoleMenus.get(rc) || []).forEach((c) => menuCodes.add(c))
  }
  const menuList = buildMenuTree().filter((m) => menuCodes.has(m.code))

  // Effective permissions
  const permCodes = new Set<string>()
  for (const rc of roleCodes) {
    ;(mockRolePermissions.get(rc) || []).forEach((c) => permCodes.add(c))
  }
  const perms = mockPermissions.filter((p) => permCodes.has(p.actionCode))

  const userWithRoles: UserWithRoles = {
    ...user,
    roles,
    groups: mockGroups,
    effectiveMenus: menuList,
    effectivePermissions: perms,
  }
  return userWithRoles
}

