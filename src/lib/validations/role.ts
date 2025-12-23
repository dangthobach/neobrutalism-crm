/**
 * ✅ PHASE 1 WEEK 3: Role Zod Validation Schemas
 */

import { z } from 'zod'

// =====================================================
// ROLE SCHEMAS
// =====================================================

/**
 * Role Status enum
 */
export const RoleStatusSchema = z.enum([
  'ACTIVE',
  'INACTIVE',
])

/**
 * Role Type enum
 */
export const RoleTypeSchema = z.enum([
  'SYSTEM',
  'CUSTOM',
])

/**
 * Permission schema
 */
export const PermissionSchema = z.object({
  resource: z
    .string()
    .min(1, 'Resource is required')
    .max(100, 'Resource must not exceed 100 characters'),

  action: z
    .string()
    .min(1, 'Action is required')
    .max(50, 'Action must not exceed 50 characters'),

  effect: z.enum(['ALLOW', 'DENY']).default('ALLOW'),

  conditions: z
    .record(z.any())
    .optional(),
})

/**
 * Role validation schema
 * Matches: com.neobrutalism.crm.domain.authorization.model.Role
 */
export const RoleSchema = z.object({
  // Required fields
  code: z
    .string()
    .min(1, 'Role code is required')
    .max(50, 'Role code must not exceed 50 characters')
    .regex(/^[A-Z0-9_-]+$/, 'Role code must contain only uppercase letters, numbers, hyphens, and underscores'),

  name: z
    .string()
    .min(1, 'Role name is required')
    .max(200, 'Role name must not exceed 200 characters'),

  status: RoleStatusSchema.default('ACTIVE'),

  roleType: RoleTypeSchema.default('CUSTOM'),

  // Optional fields
  description: z
    .string()
    .max(2000, 'Description must not exceed 2000 characters')
    .optional(),

  // Relationships
  organizationId: z
    .string()
    .uuid('Invalid organization ID')
    .optional(), // System roles don't have organizationId

  // Permissions
  permissions: z
    .array(PermissionSchema)
    .default([]),

  // Hierarchy
  parentId: z
    .string()
    .uuid('Invalid parent role ID')
    .optional(),

  level: z
    .number()
    .int('Level must be an integer')
    .min(0, 'Level must be non-negative')
    .optional(),

  path: z
    .string()
    .max(500, 'Path must not exceed 500 characters')
    .optional(),

  // Metadata
  priority: z
    .number()
    .int('Priority must be an integer')
    .min(0, 'Priority must be non-negative')
    .default(0),

  isDefault: z
    .boolean()
    .default(false),

  // Other fields
  notes: z
    .string()
    .max(5000, 'Notes must not exceed 5000 characters')
    .optional(),
})

/**
 * Create role schema
 */
export const CreateRoleSchema = RoleSchema.omit({
  level: true,
  path: true,
})

/**
 * Update role schema
 */
export const UpdateRoleSchema = RoleSchema.partial().required({
  code: true,
  name: true,
})

/**
 * System role codes
 */
export const SYSTEM_ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  USER: 'USER',
  GUEST: 'GUEST',
} as const

/**
 * Default role permissions
 */
export const DEFAULT_PERMISSIONS = {
  SUPER_ADMIN: [
    { resource: '*', action: '*', effect: 'ALLOW' },
  ],
  ADMIN: [
    { resource: 'user', action: '*', effect: 'ALLOW' },
    { resource: 'role', action: '*', effect: 'ALLOW' },
    { resource: 'group', action: '*', effect: 'ALLOW' },
    { resource: 'customer', action: '*', effect: 'ALLOW' },
  ],
  MANAGER: [
    { resource: 'customer', action: 'read', effect: 'ALLOW' },
    { resource: 'customer', action: 'update', effect: 'ALLOW' },
    { resource: 'contact', action: '*', effect: 'ALLOW' },
  ],
  USER: [
    { resource: 'customer', action: 'read', effect: 'ALLOW' },
    { resource: 'contact', action: 'read', effect: 'ALLOW' },
  ],
  GUEST: [
    { resource: 'customer', action: 'read', effect: 'ALLOW' },
  ],
} as const

// =====================================================
// TYPE EXPORTS
// =====================================================

export type Role = z.infer<typeof RoleSchema>
export type CreateRole = z.infer<typeof CreateRoleSchema>
export type UpdateRole = z.infer<typeof UpdateRoleSchema>
export type RoleStatus = z.infer<typeof RoleStatusSchema>
export type RoleType = z.infer<typeof RoleTypeSchema>
export type Permission = z.infer<typeof PermissionSchema>
export type SystemRoleCode = typeof SYSTEM_ROLES[keyof typeof SYSTEM_ROLES]
