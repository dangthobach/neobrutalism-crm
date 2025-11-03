/**
 * Content Types for CMS
 * Matching backend Content entity
 */

export enum ContentType {
  BLOG = 'BLOG',
  ARTICLE = 'ARTICLE',
  TUTORIAL = 'TUTORIAL',
  GUIDE = 'GUIDE',
  NEWS = 'NEWS',
  PAGE = 'PAGE',
}

export enum ContentStatus {
  DRAFT = 'DRAFT',
  REVIEW = 'REVIEW',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
  DELETED = 'DELETED',
}

export enum MemberTier {
  FREE = 'FREE',
  BASIC = 'BASIC',
  PREMIUM = 'PREMIUM',
  ENTERPRISE = 'ENTERPRISE',
}

export interface ContentCategory {
  id: string
  name: string
  slug: string
  description?: string
  parentId?: string
  parentName?: string
  displayOrder: number
  isActive: boolean
}

export interface ContentTag {
  id: string
  name: string
  slug: string
  description?: string
  usageCount: number
}

export interface ContentSeries {
  id: string
  name: string
  slug: string
  description?: string
  thumbnailUrl?: string
  contentCount: number
  isActive: boolean
}

export interface Content {
  id: string
  tenantId: string
  title: string
  slug: string
  summary?: string
  body: string
  featuredImageId?: string
  featuredImageUrl?: string
  contentType: ContentType
  status: ContentStatus
  publishedAt?: string
  viewCount: number
  tierRequired: MemberTier
  authorId: string
  authorName: string
  seriesId?: string
  seriesName?: string
  seriesOrder?: number
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
  categories: ContentCategory[]
  tags: ContentTag[]
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  version: number
}

export interface CreateContentRequest {
  title: string
  slug: string
  summary?: string
  body: string
  featuredImageId?: string
  contentType: ContentType
  status: ContentStatus
  publishedAt?: string
  tierRequired: MemberTier
  seriesId?: string
  seriesOrder?: number
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
  categoryIds?: string[]
  tagIds?: string[]
}

export interface UpdateContentRequest {
  title?: string
  slug?: string
  summary?: string
  body?: string
  featuredImageId?: string
  contentType?: ContentType
  status?: ContentStatus
  publishedAt?: string
  tierRequired?: MemberTier
  seriesId?: string
  seriesOrder?: number
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
  categoryIds?: string[]
  tagIds?: string[]
}

export interface ContentSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  status?: ContentStatus
  contentType?: ContentType
  authorId?: string
  categoryId?: string
  tagId?: string
  seriesId?: string
  tierRequired?: MemberTier
}

export interface CreateCategoryRequest {
  name: string
  slug: string
  description?: string
  parentId?: string
  displayOrder?: number
  isActive?: boolean
}

export interface UpdateCategoryRequest {
  name?: string
  slug?: string
  description?: string
  parentId?: string
  displayOrder?: number
  isActive?: boolean
}

export interface CreateTagRequest {
  name: string
  slug: string
  description?: string
}

export interface UpdateTagRequest {
  name?: string
  slug?: string
  description?: string
}

export interface CreateSeriesRequest {
  name: string
  slug: string
  description?: string
  thumbnailUrl?: string
  isActive?: boolean
}

export interface UpdateSeriesRequest {
  name?: string
  slug?: string
  description?: string
  thumbnailUrl?: string
  isActive?: boolean
}

export interface ContentStats {
  totalContent: number
  byStatus: Record<ContentStatus, number>
  byType: Record<ContentType, number>
  totalViews: number
  publishedToday: number
}
