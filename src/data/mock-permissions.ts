// Mock data for permission system based on backend entities

import {
  User,
  Role,
  Group,
  Menu,
  MenuTab,
  MenuScreen,
  ApiEndpoint,
  Permission,
  UserStatus,
  RoleStatus,
  GroupStatus,
  HttpMethod,
  UserWithRoles,
  MenuTreeNode,
  PermissionMatrix
} from '@/types/permission';

// Mock Users
export const mockUsers: User[] = [
  {
    id: '01932e6c-7890-7890-8901-111111111111',
    username: 'nva01',
    email: 'nguyenvana@example.com',
    firstName: 'Văn A',
    lastName: 'Nguyễn',
    phone: '0912345678',
    avatar: 'https://i.pravatar.cc/150?u=nva01',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    lastLoginAt: '2025-01-25T10:30:00Z',
    lastLoginIp: '192.168.1.100',
    failedLoginAttempts: 0,
    status: UserStatus.ACTIVE,
    createdAt: '2025-01-15T08:00:00Z',
    updatedAt: '2025-01-25T10:30:00Z',
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false
  },
  {
    id: '01932e6c-7890-7890-8901-222222222222',
    username: 'ttb02',
    email: 'tranthib@example.com',
    firstName: 'Thị B',
    lastName: 'Trần',
    phone: '0923456789',
    avatar: 'https://i.pravatar.cc/150?u=ttb02',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    lastLoginAt: '2025-01-26T09:15:00Z',
    lastLoginIp: '192.168.1.101',
    failedLoginAttempts: 0,
    status: UserStatus.ACTIVE,
    createdAt: '2025-01-16T08:00:00Z',
    updatedAt: '2025-01-26T09:15:00Z',
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false
  },
  {
    id: '01932e6c-7890-7890-8901-333333333333',
    username: 'lvc03',
    email: 'levanc@example.com',
    firstName: 'Văn C',
    lastName: 'Lê',
    phone: '0934567890',
    avatar: 'https://i.pravatar.cc/150?u=lvc03',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    lastLoginAt: '2025-01-24T14:20:00Z',
    lastLoginIp: '192.168.1.102',
    failedLoginAttempts: 1,
    status: UserStatus.ACTIVE,
    createdAt: '2025-01-17T08:00:00Z',
    updatedAt: '2025-01-24T14:20:00Z',
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false
  },
  {
    id: '01932e6c-7890-7890-8901-444444444444',
    username: 'ptd04',
    email: 'phamthid@example.com',
    firstName: 'Thị D',
    lastName: 'Phạm',
    phone: '0945678901',
    avatar: 'https://i.pravatar.cc/150?u=ptd04',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    failedLoginAttempts: 3,
    status: UserStatus.SUSPENDED,
    createdAt: '2025-01-18T08:00:00Z',
    updatedAt: '2025-01-26T11:00:00Z',
    createdBy: 'system',
    updatedBy: 'admin',
    deleted: false
  }
];

// Mock Roles
export const mockRoles: Role[] = [
  {
    id: '01932e6c-1111-1111-1111-111111111111',
    code: 'ROLE_ADMIN',
    name: 'Administrator',
    description: 'Full system access with all permissions',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    isSystem: true,
    priority: 100,
    status: RoleStatus.ACTIVE,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z',
    createdBy: 'system',
    updatedBy: 'system',
    deleted: false
  },
  {
    id: '01932e6c-1111-1111-1111-222222222222',
    code: 'ROLE_DEBT_SALE_MGR',
    name: 'Debt Sale Manager',
    description: 'Có quyền duyệt bán nợ và quản lý hồ sơ',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    isSystem: false,
    priority: 80,
    status: RoleStatus.ACTIVE,
    createdAt: '2025-01-12T08:00:00Z',
    updatedAt: '2025-01-12T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  },
  {
    id: '01932e6c-1111-1111-1111-333333333333',
    code: 'ROLE_WH_ADMIN',
    name: 'Warehouse Administrator',
    description: 'Quản trị toàn bộ hệ thống kho',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    isSystem: false,
    priority: 70,
    status: RoleStatus.ACTIVE,
    createdAt: '2025-01-12T08:00:00Z',
    updatedAt: '2025-01-12T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  },
  {
    id: '01932e6c-1111-1111-1111-444444444444',
    code: 'ROLE_CASE_VIEWER',
    name: 'Case Viewer',
    description: 'Chỉ xem thông tin hồ sơ',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    isSystem: false,
    priority: 10,
    status: RoleStatus.ACTIVE,
    createdAt: '2025-01-13T08:00:00Z',
    updatedAt: '2025-01-13T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  }
];

// Mock Groups
export const mockGroups: Group[] = [
  {
    id: '01932e6c-2222-2222-2222-111111111111',
    code: 'TEAM_DV',
    name: 'Đội Dịch Vụ',
    description: 'Đội ngũ dịch vụ khách hàng',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    level: 1,
    path: '/01932e6c-2222-2222-2222-111111111111',
    status: GroupStatus.ACTIVE,
    createdAt: '2025-01-11T08:00:00Z',
    updatedAt: '2025-01-11T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  },
  {
    id: '01932e6c-2222-2222-2222-222222222222',
    code: 'TEAM_ORC',
    name: 'Đội Thu Hồi Nợ',
    description: 'Đội ngũ thu hồi công nợ',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    level: 1,
    path: '/01932e6c-2222-2222-2222-222222222222',
    status: GroupStatus.ACTIVE,
    createdAt: '2025-01-11T08:00:00Z',
    updatedAt: '2025-01-11T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  },
  {
    id: '01932e6c-2222-2222-2222-333333333333',
    code: 'TEAM_WH',
    name: 'Đội Kho Vận',
    description: 'Đội quản lý kho và logistics',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    level: 1,
    path: '/01932e6c-2222-2222-2222-333333333333',
    status: GroupStatus.ACTIVE,
    createdAt: '2025-01-11T08:00:00Z',
    updatedAt: '2025-01-11T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  },
  {
    id: '01932e6c-2222-2222-2222-444444444444',
    code: 'TEAM_WH_NORTH',
    name: 'Kho Miền Bắc',
    description: 'Chi nhánh kho miền Bắc',
    parentId: '01932e6c-2222-2222-2222-333333333333',
    organizationId: '01932e6c-0000-0000-0000-000000000001',
    level: 2,
    path: '/01932e6c-2222-2222-2222-333333333333/01932e6c-2222-2222-2222-444444444444',
    status: GroupStatus.ACTIVE,
    createdAt: '2025-01-11T08:00:00Z',
    updatedAt: '2025-01-11T08:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
    deleted: false
  }
];

// Mock Menus
export const mockMenus: Menu[] = [
  {
    id: '01932e6c-3333-3333-3333-111111111111',
    code: 'MENU_DASHBOARD',
    name: 'Dashboard',
    icon: 'LayoutDashboard',
    level: 1,
    route: '/dashboard',
    displayOrder: 1,
    isVisible: true,
    requiresAuth: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z',
    deleted: false
  },
  {
    id: '01932e6c-3333-3333-3333-222222222222',
    code: 'MENU_DEBT_SALE',
    name: 'Debt Sale',
    icon: 'DollarSign',
    level: 1,
    route: '/debt-sale',
    displayOrder: 2,
    isVisible: true,
    requiresAuth: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z',
    deleted: false
  },
  {
    id: '01932e6c-3333-3333-3333-333333333333',
    code: 'MENU_WAREHOUSE',
    name: 'Warehouse',
    icon: 'Warehouse',
    level: 1,
    route: '/warehouse',
    displayOrder: 3,
    isVisible: true,
    requiresAuth: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z',
    deleted: false
  },
  {
    id: '01932e6c-3333-3333-3333-444444444444',
    code: 'MENU_ADMIN',
    name: 'Administration',
    icon: 'Settings',
    level: 1,
    route: '/admin',
    displayOrder: 10,
    isVisible: true,
    requiresAuth: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z',
    deleted: false
  }
];

// Mock Menu Tabs
export const mockMenuTabs: MenuTab[] = [
  {
    id: '01932e6c-4444-4444-4444-111111111111',
    code: 'TAB_DEBT_DASHBOARD',
    name: 'Dashboard',
    menuId: '01932e6c-3333-3333-3333-222222222222',
    icon: 'BarChart',
    displayOrder: 1,
    isVisible: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-4444-4444-4444-222222222222',
    code: 'TAB_DEBT_LIST',
    name: 'Debt List',
    menuId: '01932e6c-3333-3333-3333-222222222222',
    icon: 'List',
    displayOrder: 2,
    isVisible: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-4444-4444-4444-333333333333',
    code: 'TAB_WH_INVENTORY',
    name: 'Inventory',
    menuId: '01932e6c-3333-3333-3333-333333333333',
    icon: 'Package',
    displayOrder: 1,
    isVisible: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-4444-4444-4444-444444444444',
    code: 'TAB_WH_TRANSFER',
    name: 'Transfer Request',
    menuId: '01932e6c-3333-3333-3333-333333333333',
    icon: 'TruckIcon',
    displayOrder: 2,
    isVisible: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  }
];

// Mock Menu Screens
export const mockMenuScreens: MenuScreen[] = [
  {
    id: '01932e6c-5555-5555-5555-111111111111',
    code: 'SCREEN_DEBT_SALE_DASHBOARD',
    name: 'Debt Sale Dashboard',
    menuId: '01932e6c-3333-3333-3333-222222222222',
    tabId: '01932e6c-4444-4444-4444-111111111111',
    route: '/debt-sale/dashboard',
    component: 'DebtSaleDashboard',
    requiresPermission: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-5555-5555-5555-222222222222',
    code: 'SCREEN_DEBT_DETAIL',
    name: 'Debt Detail',
    menuId: '01932e6c-3333-3333-3333-222222222222',
    tabId: '01932e6c-4444-4444-4444-222222222222',
    route: '/debt-sale/:id',
    component: 'DebtDetail',
    requiresPermission: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-5555-5555-5555-333333333333',
    code: 'SCREEN_WH_INVENTORY',
    name: 'Box Inventory',
    menuId: '01932e6c-3333-3333-3333-333333333333',
    tabId: '01932e6c-4444-4444-4444-333333333333',
    route: '/warehouse/inventory',
    component: 'WarehouseInventory',
    requiresPermission: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-5555-5555-5555-444444444444',
    code: 'SCREEN_WH_TRANSFER_REQUEST',
    name: 'Transfer Request',
    menuId: '01932e6c-3333-3333-3333-333333333333',
    tabId: '01932e6c-4444-4444-4444-444444444444',
    route: '/warehouse/transfer-request',
    component: 'TransferRequest',
    requiresPermission: true,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  }
];

// Mock Permissions
export const mockPermissions: Permission[] = [
  // Debt Sale permissions
  {
    id: '01932e6c-6666-6666-6666-111111111111',
    featureId: 'SCREEN_DEBT_SALE_DASHBOARD',
    actionCode: 'VIEW_DASHBOARD',
    actionLabel: 'View Dashboard',
    description: 'Xem dashboard bán nợ',
    riskLevel: 'LOW'
  },
  {
    id: '01932e6c-6666-6666-6666-222222222222',
    featureId: 'SCREEN_DEBT_SALE_DASHBOARD',
    actionCode: 'APPROVE_DEBT_SALE',
    actionLabel: 'Approve Debt Sale',
    description: 'Phê duyệt bán nợ',
    requires: ['VIEW_DASHBOARD'],
    riskLevel: 'HIGH'
  },
  {
    id: '01932e6c-6666-6666-6666-333333333333',
    featureId: 'SCREEN_DEBT_SALE_DASHBOARD',
    actionCode: 'REJECT_DEBT_SALE',
    actionLabel: 'Reject Debt Sale',
    description: 'Từ chối hồ sơ bán nợ',
    requires: ['VIEW_DASHBOARD'],
    riskLevel: 'MEDIUM'
  },
  // Warehouse permissions
  {
    id: '01932e6c-6666-6666-6666-444444444444',
    featureId: 'SCREEN_WH_INVENTORY',
    actionCode: 'VIEW_INVENTORY',
    actionLabel: 'View Inventory',
    description: 'Xem tồn kho',
    riskLevel: 'LOW'
  },
  {
    id: '01932e6c-6666-6666-6666-555555555555',
    featureId: 'SCREEN_WH_INVENTORY',
    actionCode: 'DELETE_BOX',
    actionLabel: 'Delete Box',
    description: 'Xóa hộp hàng',
    requires: ['VIEW_INVENTORY'],
    riskLevel: 'CRITICAL'
  },
  {
    id: '01932e6c-6666-6666-6666-666666666666',
    featureId: 'SCREEN_WH_TRANSFER_REQUEST',
    actionCode: 'CREATE_REQUEST',
    actionLabel: 'Create Request',
    description: 'Tạo phiếu điều chuyển',
    riskLevel: 'MEDIUM'
  },
  {
    id: '01932e6c-6666-6666-6666-777777777777',
    featureId: 'SCREEN_WH_TRANSFER_REQUEST',
    actionCode: 'EDIT_REQUEST',
    actionLabel: 'Edit Request',
    description: 'Sửa phiếu điều chuyển',
    requires: ['CREATE_REQUEST'],
    riskLevel: 'MEDIUM'
  },
  {
    id: '01932e6c-6666-6666-6666-888888888888',
    featureId: 'SCREEN_WH_TRANSFER_REQUEST',
    actionCode: 'CANCEL_REQUEST',
    actionLabel: 'Cancel Request',
    description: 'Hủy phiếu điều chuyển',
    riskLevel: 'MEDIUM'
  },
  {
    id: '01932e6c-6666-6666-6666-999999999999',
    featureId: 'SCREEN_WH_TRANSFER_REQUEST',
    actionCode: 'APPROVE_TRANSFER',
    actionLabel: 'Approve Transfer',
    description: 'Duyệt phiếu điều chuyển',
    riskLevel: 'HIGH'
  }
];

// Mock API Endpoints
export const mockApiEndpoints: ApiEndpoint[] = [
  {
    id: '01932e6c-7777-7777-7777-111111111111',
    method: HttpMethod.GET,
    path: '/api/debt-sale/dashboard',
    tag: 'debt-sale',
    description: 'Get debt sale dashboard data',
    requiresAuth: true,
    isPublic: false,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-7777-7777-7777-222222222222',
    method: HttpMethod.POST,
    path: '/api/debt-sale/:id/approve',
    tag: 'debt-sale',
    description: 'Approve debt sale',
    requiresAuth: true,
    isPublic: false,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-7777-7777-7777-333333333333',
    method: HttpMethod.GET,
    path: '/api/warehouse/inventory',
    tag: 'warehouse',
    description: 'List inventory items',
    requiresAuth: true,
    isPublic: false,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  },
  {
    id: '01932e6c-7777-7777-7777-444444444444',
    method: HttpMethod.DELETE,
    path: '/api/warehouse/box/:id',
    tag: 'warehouse',
    description: 'Delete box',
    requiresAuth: true,
    isPublic: false,
    createdAt: '2025-01-10T08:00:00Z',
    updatedAt: '2025-01-10T08:00:00Z'
  }
];

// Mock user-role relationships
export const mockUserRoles = new Map<string, string[]>([
  ['01932e6c-7890-7890-8901-111111111111', ['ROLE_DEBT_SALE_MGR', 'ROLE_CASE_VIEWER']],
  ['01932e6c-7890-7890-8901-222222222222', ['ROLE_CASE_VIEWER']],
  ['01932e6c-7890-7890-8901-333333333333', ['ROLE_WH_ADMIN']],
  ['01932e6c-7890-7890-8901-444444444444', ['ROLE_CASE_VIEWER']]
]);

// Mock user-group relationships
export const mockUserGroups = new Map<string, string[]>([
  ['01932e6c-7890-7890-8901-111111111111', ['TEAM_DV', 'TEAM_ORC']],
  ['01932e6c-7890-7890-8901-222222222222', ['TEAM_ORC']],
  ['01932e6c-7890-7890-8901-333333333333', ['TEAM_WH', 'TEAM_WH_NORTH']],
  ['01932e6c-7890-7890-8901-444444444444', ['TEAM_DV']]
]);

// Mock role-permission relationships
export const mockRolePermissions = new Map<string, string[]>([
  ['ROLE_ADMIN', mockPermissions.map(p => p.actionCode)],
  ['ROLE_DEBT_SALE_MGR', ['VIEW_DASHBOARD', 'APPROVE_DEBT_SALE', 'REJECT_DEBT_SALE']],
  ['ROLE_WH_ADMIN', ['VIEW_INVENTORY', 'DELETE_BOX', 'CREATE_REQUEST', 'EDIT_REQUEST', 'CANCEL_REQUEST', 'APPROVE_TRANSFER']],
  ['ROLE_CASE_VIEWER', ['VIEW_DASHBOARD']]
]);

// Mock role-menu relationships
export const mockRoleMenus = new Map<string, string[]>([
  ['ROLE_ADMIN', mockMenus.map(m => m.code)],
  ['ROLE_DEBT_SALE_MGR', ['MENU_DASHBOARD', 'MENU_DEBT_SALE']],
  ['ROLE_WH_ADMIN', ['MENU_DASHBOARD', 'MENU_WAREHOUSE']],
  ['ROLE_CASE_VIEWER', ['MENU_DASHBOARD', 'MENU_DEBT_SALE']]
]);

// Helper function to get user with roles
export function getUserWithRoles(userId: string): UserWithRoles | null {
  const user = mockUsers.find(u => u.id === userId);
  if (!user) return null;

  const roleCodes = mockUserRoles.get(userId) || [];
  const roles = mockRoles.filter(r => roleCodes.includes(r.code));

  const groupCodes = mockUserGroups.get(userId) || [];
  const groups = mockGroups.filter(g => groupCodes.includes(g.code));

  // Get effective menus from all roles
  const effectiveMenuCodes = new Set<string>();
  roles.forEach(role => {
    const menuCodes = mockRoleMenus.get(role.code) || [];
    menuCodes.forEach(code => effectiveMenuCodes.add(code));
  });
  const effectiveMenus = mockMenus.filter(m => effectiveMenuCodes.has(m.code));

  // Get effective permissions from all roles
  const effectivePermissionCodes = new Set<string>();
  roles.forEach(role => {
    const permCodes = mockRolePermissions.get(role.code) || [];
    permCodes.forEach(code => effectivePermissionCodes.add(code));
  });
  const effectivePermissions = mockPermissions.filter(p =>
    effectivePermissionCodes.has(p.actionCode)
  );

  return {
    ...user,
    roles,
    groups,
    effectiveMenus,
    effectivePermissions
  };
}

// Helper function to build menu tree
export function buildMenuTree(): MenuTreeNode[] {
  const menuMap = new Map<string, MenuTreeNode>();

  // Initialize all menus
  mockMenus.forEach(menu => {
    menuMap.set(menu.id, {
      ...menu,
      children: [],
      tabs: [],
      screens: []
    });
  });

  // Add tabs to menus
  mockMenuTabs.forEach(tab => {
    const menu = menuMap.get(tab.menuId);
    if (menu) {
      menu.tabs!.push(tab);
    }
  });

  // Add screens to menus
  mockMenuScreens.forEach(screen => {
    if (screen.menuId) {
      const menu = menuMap.get(screen.menuId);
      if (menu) {
        menu.screens!.push(screen);
      }
    }
  });

  // Build tree (only root level menus for now)
  return Array.from(menuMap.values()).filter(m => !m.parentId);
}

// Helper function to get permission matrix for a role
export function getPermissionMatrixForRole(roleCode: string): PermissionMatrix[] {
  const permissionCodes = mockRolePermissions.get(roleCode) || [];

  // Group permissions by feature
  const featureMap = new Map<string, PermissionMatrix>();

  mockPermissions.forEach(permission => {
    if (!featureMap.has(permission.featureId)) {
      const screen = mockMenuScreens.find(s => s.code === permission.featureId);
      featureMap.set(permission.featureId, {
        featureId: permission.featureId,
        featureName: screen?.name || permission.featureId,
        actions: []
      });
    }

    const matrix = featureMap.get(permission.featureId)!;
    matrix.actions.push({
      permission,
      granted: permissionCodes.includes(permission.actionCode)
    });
  });

  return Array.from(featureMap.values());
}
