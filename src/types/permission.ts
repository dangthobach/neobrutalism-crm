// TypeScript interfaces based on backend entities

export enum UserStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  LOCKED = 'LOCKED',
  INACTIVE = 'INACTIVE'
}

export enum RoleStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED'
}

export enum GroupStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export enum HttpMethod {
  GET = 'GET',
  POST = 'POST',
  PUT = 'PUT',
  PATCH = 'PATCH',
  DELETE = 'DELETE'
}

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  avatar?: string;
  organizationId: string;
  lastLoginAt?: string;
  lastLoginIp?: string;
  passwordChangedAt?: string;
  failedLoginAttempts: number;
  lockedUntil?: string;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  deleted: boolean;
}

export interface Role {
  id: string;
  code: string;
  name: string;
  description?: string;
  organizationId: string;
  isSystem: boolean;
  priority: number;
  status: RoleStatus;
  createdAt: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  deleted: boolean;
}

export interface Group {
  id: string;
  code: string;
  name: string;
  description?: string;
  parentId?: string;
  organizationId: string;
  level: number;
  path: string;
  status: GroupStatus;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  deleted: boolean;
}

export interface Menu {
  id: string;
  code: string;
  name: string;
  icon?: string;
  parentId?: string;
  level: number;
  path?: string;
  route?: string;
  displayOrder: number;
  isVisible: boolean;
  requiresAuth: boolean;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}

export interface MenuTab {
  id: string;
  code: string;
  name: string;
  menuId: string;
  icon?: string;
  displayOrder: number;
  isVisible: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MenuScreen {
  id: string;
  code: string;
  name: string;
  menuId?: string;
  tabId?: string;
  route?: string;
  component?: string;
  requiresPermission: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ApiEndpoint {
  id: string;
  method: HttpMethod;
  path: string;
  tag?: string;
  description?: string;
  requiresAuth: boolean;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

// Extended types for UI
export interface UserWithRoles extends User {
  roles: Role[];
  groups: Group[];
  effectiveMenus: Menu[];
  effectivePermissions: Permission[];
}

export interface Permission {
  id: string;
  featureId: string;
  actionCode: string;
  actionLabel: string;
  description?: string;
  requires?: string[];
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export interface RolePermission {
  roleCode: string;
  permissionId: string;
  granted: boolean;
}

export interface MenuTreeNode extends Menu {
  children?: MenuTreeNode[];
  tabs?: MenuTab[];
  screens?: MenuScreen[];
  checked?: boolean;
  indeterminate?: boolean;
}

export interface PermissionMatrix {
  featureId: string;
  featureName: string;
  actions: {
    permission: Permission;
    granted: boolean;
  }[];
}

export interface EffectiveAccess {
  menus: Menu[];
  permissions: Permission[];
}

export interface PermissionAuditResult {
  permissionCode: string;
  permissionLabel: string;
  roles: {
    role: Role;
    userCount: number;
    users: User[];
  }[];
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}
