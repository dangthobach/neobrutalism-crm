/**
 * ✅ PHASE 1 WEEK 3: Branch Zod Validation Schemas
 */

import { z } from 'zod'

// =====================================================
// BRANCH SCHEMAS
// =====================================================

/**
 * Branch Type enum
 * ✅ FIXED: Matches Backend enum exactly (Branch.java:156-160)
 */
export const BranchTypeSchema = z.enum([
  'HQ',         // Head Quarter - Trụ sở chính
  'REGIONAL',   // Chi nhánh vùng
  'LOCAL',      // Chi nhánh địa phương
])

/**
 * Branch Status enum
 */
export const BranchStatusSchema = z.enum([
  'ACTIVE',
  'INACTIVE',
  'CLOSED',
])

/**
 * Branch validation schema
 * Matches: com.neobrutalism.crm.domain.branch.Branch
 */
export const BranchSchema = z.object({
  // Required fields
  code: z
    .string()
    .min(1, 'Branch code is required')
    .max(50, 'Branch code must not exceed 50 characters')
    .regex(/^[A-Z0-9_-]+$/, 'Branch code must contain only uppercase letters, numbers, hyphens, and underscores'),

  name: z
    .string()
    .min(1, 'Branch name is required')
    .max(200, 'Branch name must not exceed 200 characters'),

  branchType: BranchTypeSchema.default('LOCAL'),

  status: BranchStatusSchema.default('ACTIVE'),

  // Optional fields
  description: z
    .string()
    .max(1000, 'Description must not exceed 1000 characters')
    .optional(),

  email: z
    .string()
    .email('Invalid email address')
    .max(255, 'Email must not exceed 255 characters')
    .optional()
    .or(z.literal('')),

  phone: z
    .string()
    .max(20, 'Phone must not exceed 20 characters')
    .regex(/^[0-9+\-() ]*$/, 'Phone must contain only numbers and phone symbols')
    .optional()
    .or(z.literal('')),

  // Address
  address: z
    .string()
    .max(500, 'Address must not exceed 500 characters')
    .optional(),

  // Hierarchy fields
  parentId: z
    .string()
    .uuid('Invalid parent branch ID')
    .optional(),

  level: z
    .number()
    .int('Level must be an integer')
    .min(0, 'Level must be non-negative')
    .optional(),

  path: z
    .string()
    .max(1000, 'Path must not exceed 1000 characters')
    .optional(),

  displayOrder: z
    .number()
    .int('Display order must be an integer')
    .min(0, 'Display order must be non-negative')
    .default(0),

  // Relationships
  organizationId: z
    .string()
    .uuid('Invalid organization ID'),

  managerId: z
    .string()
    .uuid('Invalid manager ID')
    .optional(),
})

/**
 * Create branch schema
 */
export const CreateBranchSchema = BranchSchema.omit({
  level: true,
  path: true,
})

/**
 * Update branch schema
 */
export const UpdateBranchSchema = BranchSchema.partial().required({
  code: true,
  name: true,
  organizationId: true,
})

// =====================================================
// TYPE EXPORTS
// =====================================================

export type Branch = z.infer<typeof BranchSchema>
export type CreateBranch = z.infer<typeof CreateBranchSchema>
export type UpdateBranch = z.infer<typeof UpdateBranchSchema>
export type BranchType = z.infer<typeof BranchTypeSchema>
export type BranchStatus = z.infer<typeof BranchStatusSchema>
