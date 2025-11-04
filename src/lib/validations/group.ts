/**
 * ✅ PHASE 1 WEEK 3: Group Zod Validation Schemas
 */

import { z } from 'zod'

// =====================================================
// GROUP SCHEMAS
// =====================================================

/**
 * Group Type enum
 */
export const GroupTypeSchema = z.enum([
  'DEPARTMENT',
  'TEAM',
  'PROJECT',
  'LOCATION',
  'FUNCTIONAL',
  'TEMPORARY',
  'SECURITY',
  'OTHER',
])

/**
 * Group Status enum
 */
export const GroupStatusSchema = z.enum([
  'ACTIVE',
  'INACTIVE',
  'ARCHIVED',
])

/**
 * Group validation schema
 * Matches: com.neobrutalism.crm.domain.group.model.Group
 */
export const GroupSchema = z.object({
  // Required fields
  code: z
    .string()
    .min(1, 'Group code is required')
    .max(50, 'Group code must not exceed 50 characters')
    .regex(/^[A-Z0-9_-]+$/, 'Group code must contain only uppercase letters, numbers, hyphens, and underscores'),

  name: z
    .string()
    .min(1, 'Group name is required')
    .max(200, 'Group name must not exceed 200 characters'),

  groupType: GroupTypeSchema,

  status: GroupStatusSchema.default('ACTIVE'),

  // Optional fields
  description: z
    .string()
    .max(2000, 'Description must not exceed 2000 characters')
    .optional(),

  // Relationships
  organizationId: z
    .string()
    .uuid('Invalid organization ID'),

  // Hierarchy
  parentId: z
    .string()
    .uuid('Invalid parent group ID')
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

  // Manager
  managerId: z
    .string()
    .uuid('Invalid manager ID')
    .optional(),

  // Email
  email: z
    .string()
    .email('Invalid email address')
    .max(255, 'Email must not exceed 255 characters')
    .optional()
    .or(z.literal('')),

  // Location
  location: z
    .string()
    .max(200, 'Location must not exceed 200 characters')
    .optional(),

  // Metadata
  maxMembers: z
    .number()
    .int('Max members must be an integer')
    .min(0, 'Max members must be non-negative')
    .optional(),

  currentMemberCount: z
    .number()
    .int('Current member count must be an integer')
    .min(0, 'Current member count must be non-negative')
    .default(0),

  priority: z
    .number()
    .int('Priority must be an integer')
    .min(0, 'Priority must be non-negative')
    .default(0),

  // Flags
  isDefault: z
    .boolean()
    .default(false),

  allowAutoJoin: z
    .boolean()
    .default(false),

  requireApproval: z
    .boolean()
    .default(true),

  isVirtual: z
    .boolean()
    .default(false),

  // Tags
  tags: z
    .array(z.string().max(50))
    .default([]),

  // Other fields
  notes: z
    .string()
    .max(5000, 'Notes must not exceed 5000 characters')
    .optional(),
})

/**
 * Create group schema
 */
export const CreateGroupSchema = GroupSchema.omit({
  level: true,
  path: true,
  currentMemberCount: true,
})

/**
 * Update group schema
 */
export const UpdateGroupSchema = GroupSchema.partial().required({
  code: true,
  name: true,
})

/**
 * User-Group membership schema
 */
export const UserGroupSchema = z.object({
  userId: z
    .string()
    .uuid('Invalid user ID'),

  groupId: z
    .string()
    .uuid('Invalid group ID'),

  isPrimary: z
    .boolean()
    .default(false),

  joinedAt: z
    .string()
    .datetime('Invalid datetime format')
    .optional(),

  approvedBy: z
    .string()
    .uuid('Invalid approver ID')
    .optional(),

  approvedAt: z
    .string()
    .datetime('Invalid datetime format')
    .optional(),

  // Role in group
  groupRole: z
    .string()
    .max(50, 'Group role must not exceed 50 characters')
    .optional(),

  notes: z
    .string()
    .max(2000, 'Notes must not exceed 2000 characters')
    .optional(),
})

/**
 * Add user to group schema
 */
export const AddUserToGroupSchema = UserGroupSchema.pick({
  userId: true,
  groupId: true,
  isPrimary: true,
  groupRole: true,
})

/**
 * Remove user from group schema
 */
export const RemoveUserFromGroupSchema = z.object({
  userId: z
    .string()
    .uuid('Invalid user ID'),

  groupId: z
    .string()
    .uuid('Invalid group ID'),
})

// =====================================================
// TYPE EXPORTS
// =====================================================

export type Group = z.infer<typeof GroupSchema>
export type CreateGroup = z.infer<typeof CreateGroupSchema>
export type UpdateGroup = z.infer<typeof UpdateGroupSchema>
export type GroupType = z.infer<typeof GroupTypeSchema>
export type GroupStatus = z.infer<typeof GroupStatusSchema>
export type UserGroup = z.infer<typeof UserGroupSchema>
export type AddUserToGroup = z.infer<typeof AddUserToGroupSchema>
export type RemoveUserFromGroup = z.infer<typeof RemoveUserFromGroupSchema>
