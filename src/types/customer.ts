/**
 * Customer Types
 * Matching backend Customer entity
 */

export enum CustomerType {
  B2B = 'B2B',
  B2C = 'B2C',
  PARTNER = 'PARTNER',
  RESELLER = 'RESELLER',
  VENDOR = 'VENDOR',
  PROSPECT = 'PROSPECT',
}

export enum CustomerStatus {
  LEAD = 'LEAD',
  PROSPECT = 'PROSPECT',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  CHURNED = 'CHURNED',
  BLACKLISTED = 'BLACKLISTED',
}

export interface Customer {
  id: string
  code: string
  companyName: string
  legalName?: string
  customerType: CustomerType
  status: CustomerStatus
  industry?: string
  taxId?: string
  email?: string
  phone?: string
  website?: string
  billingAddress?: string
  shippingAddress?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  ownerId?: string
  ownerName?: string
  branchId?: string
  branchName?: string
  organizationId: string
  organizationName?: string
  annualRevenue?: number
  employeeCount?: number
  acquisitionDate?: string
  lastContactDate?: string
  nextFollowupDate?: string
  leadSource?: string
  creditLimit?: number
  paymentTermsDays?: number
  tags?: string[]
  notes?: string
  rating?: number
  isVip: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  version: number
}

export interface CreateCustomerRequest {
  code: string
  companyName: string
  legalName?: string
  customerType: CustomerType
  status: CustomerStatus
  industry?: string
  taxId?: string
  email?: string
  phone?: string
  website?: string
  billingAddress?: string
  shippingAddress?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  ownerId?: string
  branchId?: string
  organizationId: string
  annualRevenue?: number
  employeeCount?: number
  acquisitionDate?: string
  lastContactDate?: string
  nextFollowupDate?: string
  leadSource?: string
  creditLimit?: number
  paymentTermsDays?: number
  tags?: string[]
  notes?: string
  rating?: number
  isVip?: boolean
}

export interface UpdateCustomerRequest {
  companyName?: string
  legalName?: string
  customerType?: CustomerType
  status?: CustomerStatus
  industry?: string
  taxId?: string
  email?: string
  phone?: string
  website?: string
  billingAddress?: string
  shippingAddress?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  ownerId?: string
  branchId?: string
  annualRevenue?: number
  employeeCount?: number
  acquisitionDate?: string
  lastContactDate?: string
  nextFollowupDate?: string
  leadSource?: string
  creditLimit?: number
  paymentTermsDays?: number
  tags?: string[]
  notes?: string
  rating?: number
  isVip?: boolean
}

export interface CustomerSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  status?: CustomerStatus
  customerType?: CustomerType
  isVip?: boolean
  ownerId?: string
  branchId?: string
  organizationId?: string
}

export interface CustomerStats {
  total: number
  byStatus: Record<CustomerStatus, number>
  byType: Record<CustomerType, number>
  vipCount: number
  averageRevenue: number
}
