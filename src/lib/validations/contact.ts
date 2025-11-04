/**
 * ✅ PHASE 1 WEEK 3: Contact Zod Validation Schemas
 */

import { z } from 'zod'

// =====================================================
// CONTACT SCHEMAS
// =====================================================

/**
 * Contact Type enum
 */
export const ContactTypeSchema = z.enum([
  'PRIMARY',
  'SECONDARY',
  'TECHNICAL',
  'BILLING',
  'EMERGENCY',
])

/**
 * Contact validation schema
 * Matches: com.neobrutalism.crm.domain.contact.model.Contact
 */
export const ContactSchema = z.object({
  // Required fields
  firstName: z
    .string()
    .min(1, 'First name is required')
    .max(100, 'First name must not exceed 100 characters'),

  lastName: z
    .string()
    .min(1, 'Last name is required')
    .max(100, 'Last name must not exceed 100 characters'),

  // Optional fields
  middleName: z
    .string()
    .max(100, 'Middle name must not exceed 100 characters')
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

  mobile: z
    .string()
    .max(20, 'Mobile must not exceed 20 characters')
    .regex(/^[0-9+\-() ]*$/, 'Mobile must contain only numbers and phone symbols')
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

  contactType: ContactTypeSchema.default('SECONDARY'),

  // Address fields
  address: z
    .string()
    .max(500, 'Address must not exceed 500 characters')
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

  // Dates
  dateOfBirth: z
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

  // Relationships (Required)
  customerId: z
    .string()
    .uuid('Invalid customer ID'),

  organizationId: z
    .string()
    .uuid('Invalid organization ID')
    .optional(),

  // Other fields
  notes: z
    .string()
    .max(5000, 'Notes must not exceed 5000 characters')
    .optional(),

  tags: z
    .array(z.string().max(50, 'Tag must not exceed 50 characters'))
    .max(20, 'Maximum 20 tags allowed')
    .optional(),

  isPrimary: z
    .boolean()
    .default(false),

  isActive: z
    .boolean()
    .default(true),

  preferredContactMethod: z
    .string()
    .max(50, 'Preferred contact method must not exceed 50 characters')
    .optional(),

  socialMediaLinks: z
    .record(z.string().url('Invalid URL format'))
    .optional(),
})

/**
 * Create contact schema
 */
export const CreateContactSchema = ContactSchema

/**
 * Update contact schema
 */
export const UpdateContactSchema = ContactSchema.partial().required({
  firstName: true,
  lastName: true,
  customerId: true,
})

// =====================================================
// TYPE EXPORTS
// =====================================================

export type Contact = z.infer<typeof ContactSchema>
export type CreateContact = z.infer<typeof CreateContactSchema>
export type UpdateContact = z.infer<typeof UpdateContactSchema>
export type ContactType = z.infer<typeof ContactTypeSchema>
