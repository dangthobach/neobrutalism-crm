/**
 * ✅ PHASE 1 WEEK 3: User Zod Validation Schemas
 */

import { z } from 'zod'

// =====================================================
// USER SCHEMAS
// =====================================================

/**
 * User Status enum
 */
export const UserStatusSchema = z.enum([
  'ACTIVE',
  'INACTIVE',
  'SUSPENDED',
  'LOCKED',
])

/**
 * User validation schema
 * Matches: com.neobrutalism.crm.domain.user.model.User
 */
export const UserSchema = z.object({
  // Required fields
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must not exceed 50 characters')
    .regex(/^[a-zA-Z0-9_-]+$/, 'Username must contain only letters, numbers, hyphens, and underscores'),

  email: z
    .string()
    .email('Invalid email address')
    .max(255, 'Email must not exceed 255 characters'),

  firstName: z
    .string()
    .min(1, 'First name is required')
    .max(100, 'First name must not exceed 100 characters'),

  lastName: z
    .string()
    .min(1, 'Last name is required')
    .max(100, 'Last name must not exceed 100 characters'),

  status: UserStatusSchema.default('ACTIVE'),

  // Optional fields
  middleName: z
    .string()
    .max(100, 'Middle name must not exceed 100 characters')
    .optional(),

  displayName: z
    .string()
    .max(200, 'Display name must not exceed 200 characters')
    .optional(),

  phone: z
    .string()
    .max(20, 'Phone must not exceed 20 characters')
    .regex(/^[0-9+\-() ]*$/, 'Phone must contain only numbers and phone symbols')
    .optional()
    .or(z.literal('')),

  mobile: z
    .string()
    .max(20, 'Mobile must not exceed 20 characters')
    .regex(/^[0-9+\-() ]*$/, 'Mobile must contain only numbers and phone symbols')
    .optional()
    .or(z.literal('')),

  avatar: z
    .string()
    .url('Invalid URL format')
    .max(500, 'Avatar URL must not exceed 500 characters')
    .optional()
    .or(z.literal('')),

  jobTitle: z
    .string()
    .max(100, 'Job title must not exceed 100 characters')
    .optional(),

  department: z
    .string()
    .max(100, 'Department must not exceed 100 characters')
    .optional(),

  // Dates
  dateOfBirth: z
    .string()
    .date('Invalid date format')
    .optional(),

  hireDate: z
    .string()
    .date('Invalid date format')
    .optional(),

  // Security fields
  passwordExpiryDate: z
    .string()
    .datetime('Invalid datetime format')
    .optional(),

  lastLoginAt: z
    .string()
    .datetime('Invalid datetime format')
    .optional(),

  lastPasswordChangeAt: z
    .string()
    .datetime('Invalid datetime format')
    .optional(),

  failedLoginAttempts: z
    .number()
    .int('Failed login attempts must be an integer')
    .min(0, 'Failed login attempts must be non-negative')
    .default(0),

  accountLockedUntil: z
    .string()
    .datetime('Invalid datetime format')
    .optional(),

  // Relationships
  organizationId: z
    .string()
    .uuid('Invalid organization ID'),

  // Preferences
  locale: z
    .string()
    .max(10, 'Locale must not exceed 10 characters')
    .default('en-US'),

  timezone: z
    .string()
    .max(50, 'Timezone must not exceed 50 characters')
    .default('UTC'),

  // Flags
  isSystemUser: z
    .boolean()
    .default(false),

  isTwoFactorEnabled: z
    .boolean()
    .default(false),

  isEmailVerified: z
    .boolean()
    .default(false),

  isPhoneVerified: z
    .boolean()
    .default(false),

  // Other fields
  notes: z
    .string()
    .max(5000, 'Notes must not exceed 5000 characters')
    .optional(),
})

/**
 * Create user schema
 */
export const CreateUserSchema = UserSchema.extend({
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(100, 'Password must not exceed 100 characters')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Password must contain at least one uppercase letter, one lowercase letter, and one number'),

  confirmPassword: z
    .string()
    .min(1, 'Please confirm your password'),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
})

/**
 * Update user schema
 */
export const UpdateUserSchema = UserSchema.partial().required({
  username: true,
  email: true,
})

/**
 * Change password schema
 */
export const ChangePasswordSchema = z.object({
  currentPassword: z
    .string()
    .min(1, 'Current password is required'),

  newPassword: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(100, 'Password must not exceed 100 characters')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Password must contain at least one uppercase letter, one lowercase letter, and one number'),

  confirmPassword: z
    .string()
    .min(1, 'Please confirm your password'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
})

// =====================================================
// TYPE EXPORTS
// =====================================================

export type User = z.infer<typeof UserSchema>
export type CreateUser = z.infer<typeof CreateUserSchema>
export type UpdateUser = z.infer<typeof UpdateUserSchema>
export type ChangePassword = z.infer<typeof ChangePasswordSchema>
export type UserStatus = z.infer<typeof UserStatusSchema>
