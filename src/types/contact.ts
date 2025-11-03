/**
 * Contact Types
 * Matching backend Contact entity
 */

export enum ContactRole {
  DECISION_MAKER = 'DECISION_MAKER',
  INFLUENCER = 'INFLUENCER',
  CHAMPION = 'CHAMPION',
  EVALUATOR = 'EVALUATOR',
  GATEKEEPER = 'GATEKEEPER',
  USER = 'USER',
  OTHER = 'OTHER',
}

export enum ContactStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  DO_NOT_CONTACT = 'DO_NOT_CONTACT',
  BOUNCED = 'BOUNCED',
  UNSUBSCRIBED = 'UNSUBSCRIBED',
}

export interface Contact {
  id: string
  customerId: string
  customerName?: string
  firstName: string
  lastName: string
  fullName: string
  title?: string
  department?: string
  role: ContactRole
  status: ContactStatus
  email?: string
  phone?: string
  mobile?: string
  isPrimary: boolean
  linkedinUrl?: string
  notes?: string
  lastContactDate?: string
  nextFollowupDate?: string
  preferredContactMethod?: string
  birthday?: string
  organizationId: string
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  version: number
}

export interface CreateContactRequest {
  customerId: string
  firstName: string
  lastName: string
  title?: string
  department?: string
  role: ContactRole
  status: ContactStatus
  email?: string
  phone?: string
  mobile?: string
  isPrimary?: boolean
  linkedinUrl?: string
  notes?: string
  lastContactDate?: string
  nextFollowupDate?: string
  preferredContactMethod?: string
  birthday?: string
  organizationId: string
}

export interface UpdateContactRequest {
  firstName?: string
  lastName?: string
  title?: string
  department?: string
  role?: ContactRole
  status?: ContactStatus
  email?: string
  phone?: string
  mobile?: string
  isPrimary?: boolean
  linkedinUrl?: string
  notes?: string
  lastContactDate?: string
  nextFollowupDate?: string
  preferredContactMethod?: string
  birthday?: string
}

export interface ContactSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  customerId?: string
  role?: ContactRole
  status?: ContactStatus
  isPrimary?: boolean
}
