/**
 * Branch Types
 * Matching backend Branch entity
 */

export interface Branch {
  id: string
  code: string
  name: string
  description?: string
  address?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  phone?: string
  email?: string
  managerId?: string
  managerName?: string
  organizationId: string
  organizationName?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  version: number
}

export interface CreateBranchRequest {
  code: string
  name: string
  description?: string
  address?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  phone?: string
  email?: string
  managerId?: string
  organizationId: string
  isActive?: boolean
}

export interface UpdateBranchRequest {
  code?: string
  name?: string
  description?: string
  address?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  phone?: string
  email?: string
  managerId?: string
  isActive?: boolean
}

export interface BranchSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  organizationId?: string
  isActive?: boolean
}
