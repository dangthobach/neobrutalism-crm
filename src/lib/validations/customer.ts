/**
 * ✅ PHASE 1 WEEK 3: Zod Validation Schemas
 * Type-safe form validation matching Jakarta Bean Validation
 */

import { z } from 'zod'

// =====================================================
// CUSTOMER SCHEMAS
// =====================================================

/**
 * Customer Type enum
 */
export const CustomerTypeSchema = z.enum([
  'INDIVIDUAL',
  'SMB',
  'ENTERPRISE',
  'GOVERNMENT',
  'NONPROFIT',
])

/**
 * Customer Status enum
 */
export const CustomerStatusSchema = z.enum([
  'LEAD',
  'PROSPECT',
  'ACTIVE',
  'INACTIVE',
  'CHURNED',
  'BLACKLISTED',
])

/**
 * Customer validation schema
 * Matches: com.neobrutalism.crm.domain.customer.model.Customer
 */
export const CustomerSchema = z.object({
  // Required fields
  code: z
    .string()
    .min(1, 'Customer code is required')
    .max(50, 'Customer code must not exceed 50 characters')
    .regex(/^[A-Z0-9_-]+$/, 'Customer code must contain only uppercase letters, numbers, hyphens, and underscores'),

  companyName: z
    .string()
    .min(1, 'Company name is required')
    .max(200, 'Company name must not exceed 200 characters'),

  customerType: CustomerTypeSchema,

  status: CustomerStatusSchema.default('LEAD'),

  // Optional fields
  legalName: z
    .string()
    .max(200, 'Legal name must not exceed 200 characters')
    .optional(),

  industry: z
    .string()
    .max(100, 'Industry must not exceed 100 characters')
    .optional(),

  taxId: z
    .string()
    .max(50, 'Tax ID must not exceed 50 characters')
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

  website: z
    .string()
    .url('Invalid URL format')
    .max(255, 'Website must not exceed 255 characters')
    .optional()
    .or(z.literal('')),

  // Address fields
  billingAddress: z
    .string()
    .max(500, 'Billing address must not exceed 500 characters')
    .optional(),

  shippingAddress: z
    .string()
    .max(500, 'Shipping address must not exceed 500 characters')
    .optional(),

  city: z
    .string()
    .max(100, 'City must not exceed 100 characters')
    .optional(),

  state: z
    .string()
    .max(100, 'State must not exceed 100 characters')
    .optional(),

  country: z
    .string()
    .max(100, 'Country must not exceed 100 characters')
    .optional(),

  postalCode: z
    .string()
    .max(20, 'Postal code must not exceed 20 characters')
    .optional(),

  // Business fields
  annualRevenue: z
    .number()
    .min(0, 'Annual revenue must be positive')
    .optional(),

  employeeCount: z
    .number()
    .int('Employee count must be an integer')
    .min(0, 'Employee count must be positive')
    .optional(),

  creditLimit: z
    .number()
    .min(0, 'Credit limit must be positive')
    .optional(),

  paymentTermsDays: z
    .number()
    .int('Payment terms must be an integer')
    .min(0, 'Payment terms must be positive')
    .optional(),

  // Dates
  acquisitionDate: z
    .string()
    .date('Invalid date format')
    .optional(),

  lastContactDate: z
    .string()
    .date('Invalid date format')
    .optional(),

  nextFollowupDate: z
    .string()
    .date('Invalid date format')
    .optional(),

  // Relationships
  organizationId: z
    .string()
    .uuid('Invalid organization ID')
    .optional(),

  ownerId: z
    .string()
    .uuid('Invalid owner ID')
    .optional(),

  branchId: z
    .string()
    .uuid('Invalid branch ID')
    .optional(),

  // Other fields
  leadSource: z
    .string()
    .max(100, 'Lead source must not exceed 100 characters')
    .optional(),

  tags: z
    .array(z.string().max(50, 'Tag must not exceed 50 characters'))
    .max(20, 'Maximum 20 tags allowed')
    .optional(),

  notes: z
    .string()
    .max(5000, 'Notes must not exceed 5000 characters')
    .optional(),

  rating: z
    .number()
    .int('Rating must be an integer')
    .min(1, 'Rating must be at least 1')
    .max(5, 'Rating must not exceed 5')
    .optional(),

  isVip: z
    .boolean()
    .default(false),
})

/**
 * Create customer schema (subset of Customer schema)
 */
export const CreateCustomerSchema = CustomerSchema.omit({
  // These fields are auto-generated or set by backend
})

/**
 * Update customer schema
 */
export const UpdateCustomerSchema = CustomerSchema.partial().required({
  // Require certain fields for update
  code: true,
  companyName: true,
})

// =====================================================
// TYPE EXPORTS
// =====================================================

export type Customer = z.infer<typeof CustomerSchema>
export type CreateCustomer = z.infer<typeof CreateCustomerSchema>
export type UpdateCustomer = z.infer<typeof UpdateCustomerSchema>
export type CustomerType = z.infer<typeof CustomerTypeSchema>
export type CustomerStatus = z.infer<typeof CustomerStatusSchema>
